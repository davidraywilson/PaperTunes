/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.eink.components.EinkNowPlayingButton
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.lazy.LazyColumn
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.EinkScreen
import com.paperapps.paperui.components.DashedDivider
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow
import moe.rukamori.archivetune.eink.components.LocalEinkMenuState
import moe.rukamori.archivetune.eink.menus.EinkYouTubeSongMenu
import moe.rukamori.archivetune.eink.viewmodels.EinkSearchViewModel
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.utils.makeTimeString
import androidx.media3.exoplayer.offline.Download
import androidx.compose.ui.res.painterResource
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import moe.rukamori.archivetune.LocalDownloadUtil
import com.paperapps.paperui.components.PanoramaPager
import com.paperapps.paperui.components.PanoramaHeader
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.AppbarAction
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.mudita.mmd.components.search_bar.SearchBarDefaultsMMD
/**
 * Online YouTube Music search for the e-ink UI. A search field sits at the top; results
 * are split into Songs and Albums tabs (mirroring CalmMusic's SearchScreen). Songs play in
 * place / queue the result list; long-press opens the existing [YouTubeSongMenu] so the
 * add-to-library and add-to-playlist actions are reused. Albums open the album detail route.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EinkSearchScreen(
    navController: NavController,
    searchViewModel: EinkSearchViewModel,
) {
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val submittedQuery = searchViewModel.submittedQuery
    val songs = searchViewModel.songs
    val albums = searchViewModel.albums
    val isSearching = searchViewModel.isSearching
    val errorMessage = searchViewModel.errorMessage
    val hasSearched = searchViewModel.hasSearched

    val titles = listOf("Songs", "Albums")
    val pagerState = rememberPagerState(pageCount = { titles.size })

    // Sync selected tab with pager
    LaunchedEffect(pagerState.currentPage) {
        searchViewModel.selectedTab = pagerState.currentPage
    }
    LaunchedEffect(searchViewModel.selectedTab) {
        if (pagerState.currentPage != searchViewModel.selectedTab) {
            pagerState.animateScrollToPage(searchViewModel.selectedTab)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (!hasSearched) Modifier.padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                else Modifier
            )
    ) {
        
        if (hasSearched) {
            PanoramaHeader(
                pagerState = pagerState,
                titles = titles,
                coroutineScope = coroutineScope,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Search Box Header
        SearchBarDefaultsMMD.InputField(
            query = searchViewModel.query,
            onQueryChange = searchViewModel::updateQuery,
            onSearch = {
                keyboardController?.hide()
                searchViewModel.runSearch { keyboardController?.hide() }
            },
            expanded = true,
            onExpandedChange = { },
            placeholder = { TextMMD("Search YouTube Music") },
            trailingIcon = {
                Row {
                    if (searchViewModel.query.isNotEmpty()) {
                        IconButton(onClick = searchViewModel::clearSearch) {
                            Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Clear search")
                        }
                    }
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            searchViewModel.runSearch { keyboardController?.hide() }
                        }
                    ) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
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

                else -> PanoramaPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        SongResults(
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
                                navController.navigate(moe.rukamori.archivetune.eink.EinkScreen.NowPlaying.route)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            dropdownContent = { song, dismiss ->
                                EinkYouTubeSongMenu(
                                    song = song,
                                    navController = navController,
                                    onDismiss = dismiss,
                                )
                            }
                        )
                    } else {
                        AlbumResults(
                            albums = albums,
                            onClick = { album ->
                                navController.navigate("${EinkScreen.AlbumDetails.route}/${album.id}")
                            },
                        )
                    }
                }
            }
        }
        
        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Back",
                    onClick = { navController.navigateUp() }
                )
            ),
            menuItems = emptyList(),
            leftSlot = { EinkNowPlayingButton(navController) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongResults(
    songs: List<SongItem>,
    currentMediaId: String?,
    onPlay: (Int) -> Unit,
    onLongClick: ((SongItem) -> Unit)? = null,
    dropdownContent: (@Composable (SongItem, dismiss: () -> Unit) -> Unit)? = null,
) {
    if (songs.isEmpty()) {
        EinkEmptyState(
            title = "No songs",
            body = "Try a different search.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(end = 16.dp)) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id },
        ) { index, song ->
            EinkYouTubeSongRow(
                song = song,
                isCurrentlyPlaying = song.id == currentMediaId,
                onClick = { onPlay(index) },
                onLongClick = { onLongClick?.invoke(song) },
                dropdownContent = if (dropdownContent != null) {
                    { dismiss -> dropdownContent(song, dismiss) }
                } else null,
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
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(end = 16.dp)) {
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
    onLongClick: (() -> Unit)? = null,
    dropdownContent: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    showDivider: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val downloadUtil = LocalDownloadUtil.current
    val downloadsMap by downloadUtil.downloads.collectAsState()
    val downloadState = downloadsMap[song.id]?.state

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (dropdownContent != null) {
                            expanded = true
                        }
                        onLongClick?.invoke()
                    },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                Icon(
                                    painter = painterResource(id = moe.rukamori.archivetune.R.drawable.offline),
                                    contentDescription = "Downloaded",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                CircularProgressIndicatorMMD(
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        TextMMD(
                            text = songItemSubtitle(song),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (showDivider) DashedDivider(thickness = 1.dp)
        }

        if (dropdownContent != null) {
            DropdownMenuMMD(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 16.dp, y = 0.dp)
            ) {
                dropdownContent { expanded = false }
            }
        }
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
