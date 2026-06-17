/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
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

    val songs by viewModel.allSongs.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            EinkEmptyState(
                title = "No songs yet",
                body = "Like or download songs, or search YouTube Music to start building your library.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                itemsIndexed(
                    items = songs,
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
                                        items = songs.map { it.toMediaItem() },
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
                        showDivider = index != songs.lastIndex,
                    )
                }
            }

            FloatingActionButtonMMD(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = "Songs",
                                items = songs.shuffled().map { it.toMediaItem() },
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
