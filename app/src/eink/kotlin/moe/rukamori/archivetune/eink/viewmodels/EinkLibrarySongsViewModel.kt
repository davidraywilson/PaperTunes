package moe.rukamori.archivetune.eink.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.rukamori.archivetune.constants.SongSortType
import moe.rukamori.archivetune.db.MusicDatabase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EinkLibrarySongsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {

        // Sort preference — default NAME ascending for e-ink readability
        private val _sortType = MutableStateFlow(SongSortType.NAME)
        val sortType = _sortType.asStateFlow()

        private val _sortDescending = MutableStateFlow(false)
        val sortDescending = _sortDescending.asStateFlow()

        // Unified song list: liked OR isLocal=1 OR dateDownload IS NOT NULL
        // importSongCandidates() already returns `WHERE inLibrary IS NOT NULL OR isLocal`
        val allSongs =
            combine(_sortType, _sortDescending) { sort, desc -> sort to desc }
                .flatMapLatest { (sort, desc) ->
                    database.importSongCandidates().map { songs ->
                        // Deduplicate by title (case-insensitive). Local wins over YTM.
                        val deduplicated = songs
                            .groupBy { it.title.lowercase() }
                            .map { (title, group) ->
                                // local wins over YTM
                                group.maxByOrNull { if (it.song.isLocal) 1 else 0 } ?: group.first()
                            }

                        // Manual in-memory sort
                        val sorted = when (sort) {
                            SongSortType.NAME -> deduplicated.sortedBy { it.title.lowercase() }
                            SongSortType.CREATE_DATE -> deduplicated.sortedBy { it.song.inLibrary ?: it.song.dateModified ?: it.song.date }
                            SongSortType.PLAY_TIME -> deduplicated.sortedBy { it.song.totalPlayTime }
                            SongSortType.ARTIST -> deduplicated.sortedBy { it.artists.firstOrNull()?.name?.lowercase() ?: "" }
                        }
                        
                        if (desc) sorted.reversed() else sorted
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        fun setSortType(type: SongSortType) {
            _sortType.value = type
        }

        fun toggleSortOrder() {
            _sortDescending.value = !_sortDescending.value
        }
    }
