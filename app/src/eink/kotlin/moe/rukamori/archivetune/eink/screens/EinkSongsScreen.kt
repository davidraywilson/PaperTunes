/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkSongRow
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.eink.components.LocalEinkMenuState
import moe.rukamori.archivetune.eink.menus.EinkSongMenu
import moe.rukamori.archivetune.viewmodels.LibrarySongsViewModel

@Composable
fun EinkSongsScreen(
    navController: NavController,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val downloadUtil = LocalDownloadUtil.current

    val songs by viewModel.allSongs.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val downloadsMap by downloadUtil.downloads.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    
    val displaySongs = remember(songs, selectedTab, downloadsMap) {
        if (selectedTab == 0) songs else songs.filter { downloadsMap[it.id]?.state == Download.STATE_COMPLETED }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRowMMD(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabMMD(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { TextMMD("All Songs") }
            )
            TabMMD(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { TextMMD("Downloaded") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (displaySongs.isEmpty()) {
                EinkEmptyState(
                    title = if (selectedTab == 0) "No songs yet" else "No downloaded songs",
                    body = if (selectedTab == 0) "Like or download songs, or search YouTube Music to start building your library." else "Download some songs to listen offline.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
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
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                EinkSongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                        showDivider = index != displaySongs.lastIndex,
                    )
                }
            }
        }

        FloatingActionButtonMMD(
            modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    if (displaySongs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = "Songs",
                                items = displaySongs.shuffled().map { it.toMediaItem() },
                            ),
                        )
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "Shuffle songs",
                )
            }
        }
    }
}
