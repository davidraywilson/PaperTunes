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
import moe.rukamori.archivetune.db.MusicDatabase
import javax.inject.Inject

@HiltViewModel
class EinkArtistContentViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        
        val artistId = savedStateHandle.get<String>("artistId")!!

        // We fetch the artist entity first to get its name.
        // If it's a YouTube artist, we get its name. If it's local, we get its name.
        val artistNameFlow = database.artist(artistId).map { it?.artist?.name }

        // All songs in library that match this artist's name (case-insensitive)
        val librarySongs = artistNameFlow.flatMapLatest { name ->
            if (name == null) return@flatMapLatest flowOf(emptyList())
            val lowerName = name.lowercase()
            database.importSongCandidates().map { songs ->
                songs.filter { song ->
                    song.artists.any { it.name.lowercase() == lowerName }
                }
                .groupBy { it.title.lowercase() }
                .map { (_, group) -> group.maxByOrNull { if (it.song.isLocal) 1 else 0 } ?: group.first() }
                .sortedBy { it.song.inLibrary ?: it.song.dateModified ?: it.song.date }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // All albums in library that match this artist's name
        val libraryAlbums = artistNameFlow.flatMapLatest { name ->
            if (name == null) return@flatMapLatest flowOf(emptyList())
            val lowerName = name.lowercase()
            
            // Getting all liked albums PLUS albums derived from library songs
            combine(
                database.albumsLiked(moe.rukamori.archivetune.constants.AlbumSortType.CREATE_DATE, false),
                database.importSongCandidates()
            ) { liked, librarySongs ->
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
                    moe.rukamori.archivetune.db.entities.Album(
                        album = entity,
                        artists = albumArtistsMap[entity.id] ?: emptyList(),
                        songCountListened = 0
                    )
                }

                // Merge by case-insensitive title and FILTER by artist name
                rawAlbums
                    .filter { album -> album.artists.any { it.name.lowercase() == lowerName } }
                    .groupBy { it.title.lowercase() }
                    .map { (_, group) ->
                        val bestRepresentative = group.minByOrNull { if (it.album.isLocal) 1 else 0 } ?: group.first()
                        val representativeArtists = albumArtistsMap[bestRepresentative.id] ?: emptyList()
                        moe.rukamori.archivetune.db.entities.Album(
                            album = bestRepresentative.album,
                            artists = representativeArtists,
                            songCountListened = group.sumOf { it.songCountListened ?: 0 }
                        )
                    }
                    .sortedByDescending { it.album.year ?: 0 }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
