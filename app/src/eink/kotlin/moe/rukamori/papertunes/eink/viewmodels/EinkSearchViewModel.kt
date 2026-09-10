/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.papertunes.eink.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.rukamori.papertunes.innertube.YouTube
import moe.rukamori.papertunes.innertube.models.AlbumItem
import moe.rukamori.papertunes.innertube.models.SongItem

class EinkSearchViewModel : ViewModel() {
    var query by mutableStateOf("")
    var submittedQuery by mutableStateOf("")
    var songs by mutableStateOf<List<SongItem>>(emptyList())
    var albums by mutableStateOf<List<AlbumItem>>(emptyList())
    var isSearching by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var hasSearched by mutableStateOf(false)
    var selectedTab by mutableIntStateOf(0)

    fun updateQuery(newQuery: String) {
        query = newQuery
    }

    fun runSearch(onHideKeyboard: () -> Unit) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        onHideKeyboard()
        submittedQuery = trimmed
        isSearching = true
        errorMessage = null
        hasSearched = true
        viewModelScope.launch {
            val songResult = YouTube.search(trimmed, YouTube.SearchFilter.FILTER_SONG)
            val albumResult = YouTube.search(trimmed, YouTube.SearchFilter.FILTER_ALBUM)
            val failure = songResult.exceptionOrNull() ?: albumResult.exceptionOrNull()
            songs = songResult.getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
            albums = albumResult.getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
            errorMessage = if (songs.isEmpty() && albums.isEmpty()) failure?.localizedMessage else null
            isSearching = false
        }
    }

    fun clearSearch() {
        query = ""
    }
}
