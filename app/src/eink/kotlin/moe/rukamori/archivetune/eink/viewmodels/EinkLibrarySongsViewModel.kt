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
        // which perfectly captures all 3 categories (liked and downloaded songs both set inLibrary)
        val allSongs =
            combine(_sortType, _sortDescending) { sort, desc -> sort to desc }
                .flatMapLatest { (sort, desc) ->
                    database.importSongCandidates().map { songs ->
                        // Manual in-memory sort to fulfill the sort types
                        val sorted = when (sort) {
                            SongSortType.NAME -> songs.sortedBy { it.title.lowercase() }
                            SongSortType.CREATE_DATE -> songs.sortedBy { it.song.inLibrary ?: it.song.dateModified ?: it.song.date }
                            SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                            SongSortType.ARTIST -> songs.sortedBy { it.artists.firstOrNull()?.name?.lowercase() ?: "" }
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
