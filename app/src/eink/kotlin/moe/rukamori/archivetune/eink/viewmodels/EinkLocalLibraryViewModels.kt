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

        // artistsBookmarked: YTM-followed artists + local artists + artists from downloaded songs
        val allArtists =
            combine(
                database.artistsBookmarked(ArtistSortType.NAME, false),
                database.importSongCandidates()
            ) { bookmarked, librarySongs ->
                val artistMap = mutableMapOf<String, ArtistEntity>()
                val artistSongCount = mutableMapOf<String, Int>()

                // 1. Add bookmarked artists (these might be followed online)
                bookmarked.forEach { 
                    artistMap[it.id] = it.artist
                    artistSongCount[it.id] = it.songCount
                }

                // 2. Add artists from library songs (local + downloaded + liked)
                librarySongs.forEach { song ->
                    song.artists.forEach { artist ->
                        artistMap[artist.id] = artist
                        // We count library songs for these artists
                        // If it's a new artist, start at 1, if it existed, we might just recompute it
                    }
                }
                
                // Recompute exact library song count for ALL artists to be safe
                val computedSongCount = mutableMapOf<String, Int>()
                librarySongs.forEach { song ->
                    song.artists.forEach { artist ->
                        computedSongCount[artist.id] = (computedSongCount[artist.id] ?: 0) + 1
                    }
                }

                artistMap.values.map { entity ->
                    Artist(
                        artist = entity,
                        songCount = computedSongCount[entity.id] ?: artistSongCount[entity.id] ?: 0,
                        timeListened = 0
                    )
                }.sortedBy { it.title.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

@HiltViewModel
class EinkLibraryAlbumsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {

        // albumsLiked: YTM-bookmarked albums + local albums + albums from downloaded songs
        val allAlbums =
            combine(
                database.albumsLiked(AlbumSortType.NAME, false),
                database.importSongCandidates()
            ) { liked, librarySongs ->
                val albumMap = mutableMapOf<String, AlbumEntity>()
                val albumArtistsMap = mutableMapOf<String, List<ArtistEntity>>()

                // 1. Add explicitly liked/bookmarked albums
                liked.forEach { 
                    albumMap[it.id] = it.album
                    albumArtistsMap[it.id] = it.artists
                }

                // 2. Add albums derived from library songs
                librarySongs.forEach { song ->
                    song.album?.let { albumEntity ->
                        albumMap[albumEntity.id] = albumEntity
                        if (!albumArtistsMap.containsKey(albumEntity.id)) {
                            albumArtistsMap[albumEntity.id] = song.artists
                        }
                    }
                }

                albumMap.values.map { entity ->
                    Album(
                        album = entity,
                        artists = albumArtistsMap[entity.id] ?: emptyList(),
                        songCountListened = 0
                    )
                }.sortedBy { it.title.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
