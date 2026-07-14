/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

// CUSTOM: E-Ink Local Artists/Albums — Eink-specific ViewModels that show only local-file-derived
// artists and albums, lazily enriched with YouTube metadata by name search.
// These are fully isolated from the main app's LibraryArtistsViewModel / LibraryAlbumsViewModel.

package moe.rukamori.archivetune.eink.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.constants.AlbumSortType
import moe.rukamori.archivetune.constants.ArtistSortType
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.ArtistEntity
import moe.rukamori.archivetune.db.entities.AlbumEntity
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.AlbumItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EinkLocalArtistsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {

        // Stream of local-file-derived artists only (isLocal = 1), sorted by name.
        val allArtists =
            database
                .localArtists(ArtistSortType.NAME, false)
                .map { artists ->
                    val seenNames = mutableSetOf<String>()
                    val regex = Regex("(?i)\\s+(feat\\.|ft\\.|featuring)\\s+.*\$")
                    artists.filter { artistItem ->
                        val baseName = artistItem.artist.name.replace(regex, "").trim()
                        if (seenNames.contains(baseName.lowercase())) {
                            false
                        } else {
                            seenNames.add(baseName.lowercase())
                            true
                        }
                    }
                }
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    }

@HiltViewModel
class EinkLocalAlbumsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {

        // Stream of local-file-derived albums only (isLocal = 1), sorted by title.
        val allAlbums =
            database
                .localAlbums(AlbumSortType.NAME, false)
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    }
