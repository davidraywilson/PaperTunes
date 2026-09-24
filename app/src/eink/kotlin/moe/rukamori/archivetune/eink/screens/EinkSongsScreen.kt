/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.paperapps.paperui.components.PaperLazyColumn
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkSegmentedControl
import moe.rukamori.archivetune.eink.components.EinkSongRow
import moe.rukamori.archivetune.eink.components.EinkSongSortSheet
import moe.rukamori.archivetune.eink.menus.EinkSongMenu
import moe.rukamori.archivetune.eink.viewmodels.EinkLibrarySongsViewModel
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.playback.queues.ListQueue

@Composable
fun EinkSongsScreen(
    navController: NavController,
    onShuffleReady: ((() -> Unit)?) -> Unit = {},
    onSortActionReady: ((() -> Unit)?) -> Unit = {},
    viewModel: EinkLibrarySongsViewModel = hiltViewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val librarySongs by viewModel.allSongs.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val sortDescending by viewModel.sortDescending.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortSheet by remember { mutableStateOf(false) }

    val displaySongs = remember(librarySongs, selectedTab) {
        when (selectedTab) {
            0 -> librarySongs
            1 -> librarySongs.filter { it.song.isLocal }
            else -> librarySongs
        }
    }

    // Provide shuffle action to parent (EinkHomeScreen action bar)
    LaunchedEffect(displaySongs) {
        if (displaySongs.isNotEmpty()) {
            onShuffleReady {
                playerConnection.playQueue(
                    ListQueue(
                        title = "Songs",
                        items = displaySongs.shuffled().map { it.toMediaItem() },
                    ),
                )
            }
        } else {
            onShuffleReady(null)
        }
    }

    // Provide sort action to parent (EinkHomeScreen action bar)
    LaunchedEffect(Unit) {
        onSortActionReady { showSortSheet = true }
    }

    // Sort sheet
    if (showSortSheet) {
        EinkSongSortSheet(
            currentSort = sortType,
            isDescending = sortDescending,
            onSortSelected = { type, desc ->
                viewModel.setSortType(type)
                if (desc != sortDescending) viewModel.toggleSortOrder()
            },
            onDismiss = { showSortSheet = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkSegmentedControl(
            items = listOf("All", "Local"),
            selectedIndex = selectedTab,
            onItemSelection = { selectedTab = it },
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            if (displaySongs.isEmpty()) {
                val title = when (selectedTab) {
                    1 -> "No local songs"
                    else -> "No songs yet"
                }
                val body = when (selectedTab) {
                    1 -> "Local files will appear here once scanned."
                    else -> "Like or download songs, or search YouTube Music to start building your library."
                }
                EinkEmptyState(
                    title = title,
                    body = body,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PaperLazyColumn(modifier = Modifier.fillMaxSize(), refreshKey = displaySongs) {
                    itemsIndexed(
                        items = displaySongs,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        EinkSongRow(
                            song = song,
                            isCurrentlyPlaying = song.id == mediaMetadata?.id,
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "Songs",
                                            items = displaySongs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                }
                                navController.navigate(moe.rukamori.archivetune.eink.EinkScreen.NowPlaying.route)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            dropdownContent = { dismiss ->
                                EinkSongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = dismiss,
                                )
                            },
                            showDivider = index != displaySongs.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
