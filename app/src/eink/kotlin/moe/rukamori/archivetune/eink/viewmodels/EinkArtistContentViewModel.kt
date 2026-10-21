package moe.rukamori.archivetune.eink.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.rukamori.archivetune.playback.DownloadUtil
import androidx.media3.exoplayer.offline.Download
import moe.rukamori.archivetune.db.LocalMusicIdentity
import moe.rukamori.archivetune.eink.viewmodels.EinkAlbumUiModel
import moe.rukamori.archivetune.db.MusicDatabase
import javax.inject.Inject

@HiltViewModel
class EinkArtistContentViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        private val downloadUtil: DownloadUtil,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        
        val artistId = savedStateHandle.get<String>("artistId")!!

        // We fetch the artist entity first to get its name.
        val artistNameFlow = database.artist(artistId).map { it?.artist?.name }

        // All songs in library that match this artist's name (case-insensitive)
        val librarySongs = artistNameFlow.flatMapLatest { name ->
            if (name == null) return@flatMapLatest flowOf(emptyList())
            val normalizedName = LocalMusicIdentity.normalize(name)
            database.importSongCandidates().map { songs ->
                songs.filter { song ->
                    song.artists.any { LocalMusicIdentity.normalize(it.name) == normalizedName }
                }
                .groupBy { LocalMusicIdentity.normalize(it.title) }
                .map { (_, group) -> group.maxByOrNull { if (it.song.isLocal) 1 else 0 } ?: group.first() }
                .sortedBy { it.song.inLibrary ?: it.song.dateModified ?: it.song.date }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // All albums in library that match this artist's name
        val libraryAlbums = artistNameFlow.flatMapLatest { name ->
            if (name == null) return@flatMapLatest flowOf(emptyList())
            val normalizedName = LocalMusicIdentity.normalize(name)
            
            // Getting all liked albums PLUS albums derived from library songs
            combine(
                database.albumsLiked(moe.rukamori.archivetune.constants.AlbumSortType.CREATE_DATE, false),
                database.importSongCandidates(),
                downloadUtil.downloads
            ) { liked, librarySongs, downloads ->
                val allEntities = mutableMapOf<String, moe.rukamori.archivetune.db.entities.AlbumEntity>()
                val albumArtistsMap = mutableMapOf<String, List<moe.rukamori.archivetune.db.entities.ArtistEntity>>()

                liked.forEach { 
                    allEntities[it.id] = it.album
                    albumArtistsMap[it.id] = it.artists
                }

                librarySongs.forEach { song ->
                    song.album?.let { albumEntity ->
                        allEntities[albumEntity.id] = albumEntity
                        if (!albumArtistsMap.containsKey(albumEntity.id)) {
                            albumArtistsMap[albumEntity.id] = song.artists
                        }
                    }
                }

                val rawAlbums = allEntities.values.map { entity ->
                    val songsForAlbum = librarySongs.filter { it.album?.id == entity.id }
                    
                    val hasLocal = entity.isLocal || songsForAlbum.any { it.song.isLocal }
                    val hasDownloaded = songsForAlbum.any { !it.song.isLocal && downloads[it.id]?.state == Download.STATE_COMPLETED }
                    val hasOnline = (!entity.isLocal && songsForAlbum.isEmpty()) || 
                                    songsForAlbum.any { !it.song.isLocal && downloads[it.id]?.state != Download.STATE_COMPLETED }

                    EinkAlbumUiModel(
                        album = moe.rukamori.archivetune.db.entities.Album(
                            album = entity,
                            artists = albumArtistsMap[entity.id] ?: emptyList(),
                            songCountListened = 0
                        ),
                        hasLocal = hasLocal,
                        hasOnline = hasOnline,
                        hasDownloaded = hasDownloaded
                    )
                }

                // Merge by normalized title and FILTER by artist name
                rawAlbums
                    .filter { album -> album.album.artists.any { LocalMusicIdentity.normalize(it.name) == normalizedName } }
                    .groupBy { LocalMusicIdentity.normalize(it.album.title) }
                    .map { (_, group) ->
                        val bestRepresentative = group.maxByOrNull { if (it.album.album.isLocal) 1 else 0 } ?: group.first()
                        val representativeArtists = albumArtistsMap[bestRepresentative.album.id] ?: emptyList()
                        
                        EinkAlbumUiModel(
                            album = moe.rukamori.archivetune.db.entities.Album(
                                album = bestRepresentative.album.album,
                                artists = representativeArtists,
                                songCountListened = group.sumOf { it.album.songCountListened ?: 0 }
                            ),
                            hasLocal = group.any { it.hasLocal },
                            hasOnline = group.any { it.hasOnline },
                            hasDownloaded = group.any { it.hasDownloaded }
                        )
                    }
                    .sortedByDescending { it.album.album.year ?: 0 }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
