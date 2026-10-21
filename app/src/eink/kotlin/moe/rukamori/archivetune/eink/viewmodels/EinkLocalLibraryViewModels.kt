package moe.rukamori.archivetune.eink.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import moe.rukamori.archivetune.constants.AlbumSortType
import moe.rukamori.archivetune.constants.ArtistSortType
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Album
import moe.rukamori.archivetune.db.entities.AlbumEntity
import moe.rukamori.archivetune.db.entities.Artist
import moe.rukamori.archivetune.playback.DownloadUtil
import androidx.media3.exoplayer.offline.Download
import moe.rukamori.archivetune.db.LocalMusicIdentity
import moe.rukamori.archivetune.db.entities.ArtistEntity
import javax.inject.Inject

data class EinkArtistUiModel(
    val artist: Artist,
    val hasLocal: Boolean,
    val hasOnline: Boolean,
    val hasDownloaded: Boolean
)

data class EinkAlbumUiModel(
    val album: Album,
    val hasLocal: Boolean,
    val hasOnline: Boolean,
    val hasDownloaded: Boolean
)

@HiltViewModel
class EinkLibraryArtistsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        private val downloadUtil: DownloadUtil,
    ) : ViewModel() {

        val allArtists =
            combine(
                database.artistsBookmarked(ArtistSortType.NAME, false),
                database.importSongCandidates(),
                downloadUtil.downloads
            ) { bookmarked, librarySongs, downloads ->
                val allEntities = mutableMapOf<String, ArtistEntity>()
                val artistSongCount = mutableMapOf<String, Int>()

                // Add from bookmarked
                bookmarked.forEach { 
                    allEntities[it.id] = it.artist
                    artistSongCount[it.id] = it.songCount
                }

                // Add from library songs
                librarySongs.forEach { song ->
                    song.artists.forEach { artist ->
                        allEntities[artist.id] = artist
                    }
                }

                // First create the raw unmerged list
                val rawArtists = allEntities.values.map { entity ->
                    val songsForArtist = librarySongs.filter { song -> song.artists.any { it.id == entity.id } }
                    
                    val hasLocal = entity.isLocal || songsForArtist.any { it.song.isLocal }
                    val hasDownloaded = songsForArtist.any { !it.song.isLocal && downloads[it.id]?.state == Download.STATE_COMPLETED }
                    val hasOnline = (entity.isYouTubeArtist && songsForArtist.isEmpty()) || 
                                    songsForArtist.any { !it.song.isLocal && downloads[it.id]?.state != Download.STATE_COMPLETED }
                    
                    EinkArtistUiModel(
                        artist = Artist(
                            artist = entity,
                            songCount = songsForArtist.size.takeIf { it > 0 } ?: artistSongCount[entity.id] ?: 0,
                            timeListened = 0
                        ),
                        hasLocal = hasLocal,
                        hasOnline = hasOnline,
                        hasDownloaded = hasDownloaded
                    )
                }

                // Now MERGE by industry standard normalized name
                rawArtists
                    .groupBy { LocalMusicIdentity.normalize(it.artist.title) }
                    .map { (name, group) ->
                        // Prioritize Local artists over YouTube ones
                        val bestRepresentative = group.maxByOrNull { if (it.artist.artist.isLocal) 1 else 0 } ?: group.first()
                        
                        val totalSongs = group.sumOf { it.artist.songCount }
                        
                        EinkArtistUiModel(
                            artist = Artist(
                                artist = bestRepresentative.artist.artist,
                                songCount = totalSongs,
                                timeListened = group.sumOf { it.artist.timeListened ?: 0 }
                            ),
                            hasLocal = group.any { it.hasLocal },
                            hasOnline = group.any { it.hasOnline },
                            hasDownloaded = group.any { it.hasDownloaded }
                        )
                    }
                    .sortedBy { it.artist.title.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

@HiltViewModel
class EinkLibraryAlbumsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        private val downloadUtil: DownloadUtil,
    ) : ViewModel() {

        val allAlbums =
            combine(
                database.albumsLiked(AlbumSortType.NAME, false),
                database.importSongCandidates(),
                downloadUtil.downloads
            ) { liked, librarySongs, downloads ->
                val allEntities = mutableMapOf<String, AlbumEntity>()
                val albumArtistsMap = mutableMapOf<String, List<ArtistEntity>>()

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
                        album = Album(
                            album = entity,
                            artists = albumArtistsMap[entity.id] ?: emptyList(),
                            songCountListened = 0
                        ),
                        hasLocal = hasLocal,
                        hasOnline = hasOnline,
                        hasDownloaded = hasDownloaded
                    )
                }

                // Merge by normalized title
                rawAlbums
                    .groupBy { LocalMusicIdentity.normalize(it.album.title) }
                    .map { (title, group) ->
                        // Prefer local albums over online ones
                        val bestRepresentative = group.maxByOrNull { if (it.album.album.isLocal) 1 else 0 } ?: group.first()
                        
                        val representativeArtists = albumArtistsMap[bestRepresentative.album.id] ?: emptyList()
                        
                        EinkAlbumUiModel(
                            album = Album(
                                album = bestRepresentative.album.album,
                                artists = representativeArtists,
                                songCountListened = group.sumOf { it.album.songCountListened ?: 0 }
                            ),
                            hasLocal = group.any { it.hasLocal },
                            hasOnline = group.any { it.hasOnline },
                            hasDownloaded = group.any { it.hasDownloaded }
                        )
                    }
                    .sortedBy { it.album.title.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
