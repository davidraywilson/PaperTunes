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
import moe.rukamori.archivetune.db.entities.ArtistEntity
import javax.inject.Inject

@HiltViewModel
class EinkLibraryArtistsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {

        val allArtists =
            combine(
                database.artistsBookmarked(ArtistSortType.NAME, false),
                database.importSongCandidates()
            ) { bookmarked, librarySongs ->
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

                // Recompute accurate library song count for artists
                val computedSongCount = mutableMapOf<String, Int>()
                librarySongs.forEach { song ->
                    song.artists.forEach { artist ->
                        computedSongCount[artist.id] = (computedSongCount[artist.id] ?: 0) + 1
                    }
                }

                // First create the raw unmerged list
                val rawArtists = allEntities.values.map { entity ->
                    Artist(
                        artist = entity,
                        songCount = computedSongCount[entity.id] ?: artistSongCount[entity.id] ?: 0,
                        timeListened = 0
                    )
                }

                // Now MERGE by case-insensitive name
                rawArtists
                    .groupBy { it.title.lowercase() }
                    .map { (name, group) ->
                        // Prioritize YouTube artists (isLocal == false) over Local ones
                        val bestRepresentative = group.minByOrNull { if (it.artist.isLocal) 1 else 0 } ?: group.first()
                        
                        // Sum up the song counts of all merged entities
                        // Note: If an artist has no songs (just bookmarked), this safely carries over 0
                        val totalSongs = group.sumOf { it.songCount }
                        
                        Artist(
                            artist = bestRepresentative.artist,
                            songCount = totalSongs,
                            timeListened = group.sumOf { it.timeListened ?: 0 }
                        )
                    }
                    .sortedBy { it.title.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

@HiltViewModel
class EinkLibraryAlbumsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {

        val allAlbums =
            combine(
                database.albumsLiked(AlbumSortType.NAME, false),
                database.importSongCandidates()
            ) { liked, librarySongs ->
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
                    Album(
                        album = entity,
                        artists = albumArtistsMap[entity.id] ?: emptyList(),
                        songCountListened = 0
                    )
                }

                // Merge by case-insensitive title
                rawAlbums
                    .groupBy { it.title.lowercase() }
                    .map { (title, group) ->
                        // Prefer online/YouTube albums over local ones if duplicates exist
                        val bestRepresentative = group.minByOrNull { if (it.album.isLocal) 1 else 0 } ?: group.first()
                        
                        // Pick the artists from the representative
                        val representativeArtists = albumArtistsMap[bestRepresentative.id] ?: emptyList()
                        
                        Album(
                            album = bestRepresentative.album,
                            artists = representativeArtists,
                            songCountListened = group.sumOf { it.songCountListened ?: 0 }
                        )
                    }
                    .sortedBy { it.title.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
