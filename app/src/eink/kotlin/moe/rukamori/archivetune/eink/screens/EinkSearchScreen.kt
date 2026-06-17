/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.EinkScreen
import moe.rukamori.archivetune.eink.components.DashedDivider
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.eink.components.LocalEinkMenuState
import moe.rukamori.archivetune.eink.menus.EinkYouTubeSongMenu
import moe.rukamori.archivetune.utils.makeTimeString

/**
 * Online YouTube Music search for the e-ink UI. A search field sits at the top; results
 * are split into Songs and Albums tabs (mirroring CalmMusic's SearchScreen). Songs play in
 * place / queue the result list; long-press opens the existing [YouTubeSongMenu] so the
 * add-to-library and add-to-playlist actions are reused. Albums open the album detail route.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EinkSearchScreen(navController: NavController) {
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    fun runSearch() {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        keyboardController?.hide()
        submittedQuery = trimmed
        isSearching = true
        errorMessage = null
        hasSearched = true
        scope.launch {
            val songResult = YouTube.search(trimmed, YouTube.SearchFilter.FILTER_SONG)
            val albumResult = YouTube.search(trimmed, YouTube.SearchFilter.FILTER_ALBUM)
            val failure = songResult.exceptionOrNull() ?: albumResult.exceptionOrNull()
            songs = songResult.getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
            albums = albumResult.getOrNull()?.items?.filterIsInstance<AlbumItem>().orEmpty()
            errorMessage = if (songs.isEmpty() && albums.isEmpty()) failure?.localizedMessage else null
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = { Text(text = "Search YouTube Music") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear",
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
        )

        if (hasSearched) {
            PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
                TabMMD(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        TextMMD(
                            text = "Songs",
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
                TabMMD(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        TextMMD(
                            text = "Albums",
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !hasSearched -> EinkEmptyState(
                    title = "Search YouTube Music",
                    body = "Find songs and albums to play, add to your library, or save to a playlist.",
                    modifier = Modifier.fillMaxSize(),
                )

                isSearching -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = "Searching\u2026", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }

                errorMessage != null -> EinkEmptyState(
                    title = "Search failed",
                    body = errorMessage.orEmpty(),
                    modifier = Modifier.fillMaxSize(),
                )

                selectedTab == 0 -> SongResults(
                    songs = songs,
                    currentMediaId = mediaMetadata?.id,
                    onPlay = { index ->
                        val song = songs[index]
                        if (song.id == mediaMetadata?.id) {
                            playerConnection.player.togglePlayPause()
                        } else {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = submittedQuery,
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index,
                                ),
                            )
                        }
                    },
                    onLongClick = { song ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            EinkYouTubeSongMenu(
                                song = song,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                )

                else -> AlbumResults(
                    albums = albums,
                    onClick = { album ->
                        navController.navigate("${EinkScreen.AlbumDetails.route}/${album.id}")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongResults(
    songs: List<SongItem>,
    currentMediaId: String?,
    onPlay: (Int) -> Unit,
    onLongClick: (SongItem) -> Unit,
) {
    if (songs.isEmpty()) {
        EinkEmptyState(
            title = "No songs",
            body = "Try a different search.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id },
        ) { index, song ->
            EinkYouTubeSongRow(
                song = song,
                isCurrentlyPlaying = song.id == currentMediaId,
                onClick = { onPlay(index) },
                onLongClick = { onLongClick(song) },
                showDivider = index != songs.lastIndex,
            )
        }
    }
}

@Composable
private fun AlbumResults(
    albums: List<AlbumItem>,
    onClick: (AlbumItem) -> Unit,
) {
    if (albums.isEmpty()) {
        EinkEmptyState(
            title = "No albums",
            body = "Try a different search.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(
            items = albums,
            key = { _, album -> album.id },
        ) { index, album ->
            EinkTwoLineRow(
                title = album.title,
                subtitle = albumItemSubtitle(album),
                onClick = { onClick(album) },
                showDivider = index != albums.lastIndex,
            )
        }
    }
}

/**
 * Song row for online ([SongItem]) results. Mirrors the shared EinkSongRow look (bold 20sp
 * title, 16sp subtitle, now-playing headphones icon, trailing dashed divider) but accepts a
 * YouTube [SongItem] instead of a local DB entity.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EinkYouTubeSongRow(
    song: SongItem,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isCurrentlyPlaying) {
                Icon(
                    imageVector = Icons.Outlined.Headphones,
                    contentDescription = "Now playing",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 4.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                TextMMD(
                    text = song.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextMMD(
                    text = songItemSubtitle(song),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showDivider) DashedDivider(thickness = 1.dp)
    }
}

/** Builds the "Artist \u2022 Album \u2022 Duration" subtitle for a YouTube [SongItem]. */
private fun songItemSubtitle(song: SongItem): String {
    val artist = song.artists.joinToString(", ") { it.name }
    val album = song.album?.name?.takeIf { it.isNotBlank() }
    val duration = song.duration?.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) }
    return buildString {
        if (artist.isNotBlank()) append(artist)
        if (!album.isNullOrBlank()) {
            if (isNotEmpty()) append(" \u2022 ")
            append(album)
        }
        if (!duration.isNullOrBlank()) {
            if (isNotEmpty()) append(" \u2022 ")
            append(duration)
        }
    }
}

/** Builds the artist (and year) subtitle for a YouTube [AlbumItem]. */
private fun albumItemSubtitle(album: AlbumItem): String {
    val artist = album.artists?.joinToString(", ") { it.name }.orEmpty()
    val year = album.year?.toString()
    return buildString {
        if (artist.isNotBlank()) append(artist)
        if (!year.isNullOrBlank()) {
            if (isNotEmpty()) append(" \u2022 ")
            append(year)
        }
    }
}
