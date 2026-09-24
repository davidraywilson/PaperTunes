/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

// CUSTOM: E-Ink Library Artists/Albums — Eink-specific ViewModels that show merged library
// artists and albums, using the database's built-in deduplication.
//
// artistsBookmarked(): bookmarkedAt IS NOT NULL (YTM followed) OR isLocal=1 (local) OR
//   has a downloaded song — maps exactly to "artists I actively have in my library"
//
// albumsLiked(): bookmarkedAt IS NOT NULL (bookmarked/liked) OR isLocal=1 (local) OR
//   has a downloaded song — same semantic for albums. Downloaded albums get bookmarkedAt
//   set automatically by DownloadUtil when a song is first persisted.

package moe.rukamori.archivetune.eink.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import moe.rukamori.archivetune.constants.AlbumSortType
import moe.rukamori.archivetune.constants.ArtistSortType
import moe.rukamori.archivetune.db.MusicDatabase
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
            database
                .artistsBookmarked(ArtistSortType.NAME, false)
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
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
            database
                .albumsLiked(AlbumSortType.NAME, false)
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
