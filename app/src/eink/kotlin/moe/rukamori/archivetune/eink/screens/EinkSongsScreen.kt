/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Done
import androidx.compose.foundation.lazy.LazyColumn
import com.mudita.mmd.components.chips.FilterChipDefaultsMMD
import com.mudita.mmd.components.chips.FilterChipMMD
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
    localViewModel: moe.rukamori.archivetune.viewmodels.LocalSongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val downloadUtil = LocalDownloadUtil.current

    val librarySongs by viewModel.allSongs.collectAsState()
    val localSongs by localViewModel.songs.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val downloadsMap by downloadUtil.downloads.collectAsState()

    val allSongsMixed = remember(librarySongs, localSongs) {
        (librarySongs + localSongs).distinctBy { it.id }.sortedBy { it.song.title }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    
    val displaySongs = remember(allSongsMixed, selectedTab, downloadsMap) {
        when (selectedTab) {
            0 -> allSongsMixed
            1 -> allSongsMixed.filter { it.song.isLocal }
            2 -> allSongsMixed.filter { downloadsMap[it.id]?.state == Download.STATE_COMPLETED }
            else -> allSongsMixed
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, bottom = 8.dp)
        ) {
            FilterChipMMD(
                onClick = { selectedTab = 0 },
                label = { TextMMD("All", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp) },
                selected = selectedTab == 0,
                leadingIcon = if (selectedTab == 0) {
                    {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(com.mudita.mmd.components.chips.FilterChipDefaultsMMD.IconSize)
                        )
                    }
                } else null
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            FilterChipMMD(
                onClick = { selectedTab = 1 },
                label = { TextMMD("Local", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp) },
                selected = selectedTab == 1,
                leadingIcon = if (selectedTab == 1) {
                    {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(com.mudita.mmd.components.chips.FilterChipDefaultsMMD.IconSize)
                        )
                    }
                } else null
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            FilterChipMMD(
                onClick = { selectedTab = 2 },
                label = { TextMMD("Downloaded", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp) },
                selected = selectedTab == 2,
                leadingIcon = if (selectedTab == 2) {
                    {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(com.mudita.mmd.components.chips.FilterChipDefaultsMMD.IconSize)
                        )
                    }
                } else null
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (displaySongs.isEmpty()) {
                val title = when (selectedTab) {
                    1 -> "No local songs"
                    2 -> "No downloaded songs"
                    else -> "No songs yet"
                }
                val body = when (selectedTab) {
                    1 -> "Local files will appear here once scanned."
                    2 -> "Download some songs to listen offline."
                    else -> "Like or download songs, or search YouTube Music to start building your library."
                }
                EinkEmptyState(
                    title = title,
                    body = body,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(end = 16.dp)) {
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
