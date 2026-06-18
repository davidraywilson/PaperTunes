/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Playlist
import moe.rukamori.archivetune.db.entities.PlaylistSongMap
import moe.rukamori.archivetune.eink.einkPlaylistAddSongsRoute
import moe.rukamori.archivetune.eink.einkPlaylistDetailsRoute
import moe.rukamori.archivetune.eink.components.DashedDivider
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkSelectableRow
import moe.rukamori.archivetune.eink.components.EinkSongRow
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow
import moe.rukamori.archivetune.eink.components.playlistSubtitle
import moe.rukamori.archivetune.eink.components.songSubtitle
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.eink.components.EinkCreatePlaylistDialog
import moe.rukamori.archivetune.eink.components.EinkEditPlaylistDialog
import moe.rukamori.archivetune.eink.components.LocalEinkMenuState
import moe.rukamori.archivetune.eink.menus.EinkSongMenu
import moe.rukamori.archivetune.viewmodels.LibraryPlaylistsViewModel
import moe.rukamori.archivetune.viewmodels.LibrarySongsViewModel
import moe.rukamori.archivetune.viewmodels.LocalPlaylistViewModel
import java.time.LocalDateTime

/*
 * Owned by the `playlists` child agent. E-ink playlist screens replicating the CalmTunes
 * Playlists / PlaylistDetails / PlaylistEdit / PlaylistAddSongs flows while wiring into
 * ArchiveTune's existing playlist backend (Room + ViewModels).
 */

@Composable
fun EinkPlaylistsScreen(
    navController: NavController,
    isInEditMode: Boolean = false,
    onSelectionChanged: (Set<String>) -> Unit = {},
    showDeleteConfirmation: Boolean = false,
    onDeleteConfirmed: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val playlists by viewModel.allPlaylists.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        EinkCreatePlaylistDialog(onDismiss = { showCreateDialog = false })
    }

    if (showDeleteConfirmation) {
        val toDelete = playlists.filter { selectedIds.contains(it.id) }
        ModalBottomSheetMMD(onDismissRequest = onCancelDelete) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                TextMMD(
                    text = if (toDelete.size == 1) {
                        "Delete \"${toDelete.first().playlist.name}\"?"
                    } else {
                        "Delete ${toDelete.size} playlists?"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextMMD(
                    text = "This removes the playlist from your library. Songs stay in your library.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButtonMMD(
                        onClick = onCancelDelete,
                        modifier = Modifier.weight(1f),
                    ) {
                        TextMMD(text = "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ButtonMMD(
                        onClick = {
                            deletePlaylists(database, coroutineScope, toDelete)
                            onDeleteConfirmed()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        TextMMD(text = "Delete")
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextMMD(
                        text = "No playlists in your library yet",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ButtonMMD(onClick = { showCreateDialog = true }) {
                        TextMMD(text = "New playlist")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumnMMD(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    itemsIndexed(
                        items = playlists,
                        key = { _, playlist -> playlist.id },
                    ) { index, playlist ->
                        if (isInEditMode) {
                            EinkSelectableRow(
                                title = playlist.playlist.name,
                                subtitle = playlistSubtitle(playlist),
                                checked = selectedIds.contains(playlist.id),
                                onCheckedChange = { checked ->
                                    val newSelection = selectedIds.toMutableSet()
                                    if (checked) {
                                        newSelection.add(playlist.id)
                                    } else {
                                        newSelection.remove(playlist.id)
                                    }
                                    onSelectionChanged(newSelection)
                                },
                                showDivider = index != playlists.lastIndex,
                            )
                        } else {
                            EinkTwoLineRow(
                                title = playlist.playlist.name,
                                subtitle = playlistSubtitle(playlist),
                                onClick = {
                                    navController.navigate(einkPlaylistDetailsRoute(playlist.id))
                                },
                                showDivider = index != playlists.lastIndex,
                            )
                        }
                    }
                }
            }
        }

        if (playlists.isNotEmpty() && !isInEditMode) {
            FloatingActionButtonMMD(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = { showCreateDialog = true },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New playlist",
                )
            }
        }
    }
}

@Composable
fun EinkPlaylistDetailsScreen(
    navController: NavController,
    playlistId: String,
    showRenameDialog: Boolean = false,
    onRenameDialogDismiss: () -> Unit = {},
    showDeleteSheet: Boolean = false,
    onDeleteSheetDismiss: () -> Unit = {},
    isInEditMode: Boolean = false,
    onSelectionChanged: (Set<String>) -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistName = playlist?.playlist?.name ?: "Playlist"

    if (showRenameDialog) {
        playlist?.let { current ->
            EinkEditPlaylistDialog(
                initialName = current.playlist.name,
                onDismiss = onRenameDialogDismiss,
                onSave = { name ->
                    database.query {
                        update(
                            current.playlist.copy(
                                name = name,
                                lastUpdateTime = LocalDateTime.now(),
                            ),
                        )
                    }
                    coroutineScope.launch(Dispatchers.IO) {
                        current.playlist.browseId?.let { runCatching { YouTube.renamePlaylist(it, name) } }
                    }
                    onRenameDialogDismiss()
                },
            )
        }
    }

    if (showDeleteSheet) {
        if (isInEditMode) {
            // Delete selected songs
            val toDelete = songs.filter { selectedIds.contains(it.map.id.toString()) } // Note: using map.id? It's Long. Just using song.id. Wait.
            ModalBottomSheetMMD(onDismissRequest = onDeleteSheetDismiss) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    TextMMD(
                        text = "Remove ${toDelete.size} songs?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButtonMMD(
                            onClick = onDeleteSheetDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            TextMMD(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ButtonMMD(
                            onClick = {
                                toDelete.forEach { map ->
                                    removeSong(database, coroutineScope, map.map)
                                }
                                onDeleteSheetDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            TextMMD(text = "Remove")
                        }
                    }
                }
            }
        } else {
            // Delete Playlist
            ModalBottomSheetMMD(onDismissRequest = onDeleteSheetDismiss) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    TextMMD(
                        text = "Delete \"$playlistName\"?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButtonMMD(
                            onClick = onDeleteSheetDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            TextMMD(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ButtonMMD(
                            onClick = {
                                val current = playlist
                                database.query { current?.let { delete(it.playlist) } }
                                coroutineScope.launch(Dispatchers.IO) {
                                    current?.playlist?.browseId?.let { runCatching { YouTube.deletePlaylist(it) } }
                                }
                                onDeleteSheetDismiss()
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            TextMMD(text = "Delete")
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (songs.isEmpty()) {
                EinkEmptyState(
                    title = "No songs in this playlist",
                    body = "Use Add songs to put tracks from your library here.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumnMMD(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, playlistSong -> playlistSong.map.id },
                    ) { index, playlistSong ->
                        val song = playlistSong.song
                        if (isInEditMode) {
                            EinkSelectableRow(
                                title = song.song.title,
                                subtitle = songSubtitle(song),
                                checked = selectedIds.contains(playlistSong.map.id.toString()),
                                onCheckedChange = { checked ->
                                    val newSelection = selectedIds.toMutableSet()
                                    if (checked) {
                                        newSelection.add(playlistSong.map.id.toString())
                                    } else {
                                        newSelection.remove(playlistSong.map.id.toString())
                                    }
                                    onSelectionChanged(newSelection)
                                },
                                showDivider = index != songs.lastIndex,
                            )
                        } else {
                            EinkSongRow(
                                song = song,
                                isCurrentlyPlaying = song.id == mediaMetadata?.id,
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = playlistName,
                                                items = songs.map { it.song.toMediaItem() },
                                                startIndex = index,
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                dropdownContent = { dismiss ->
                                    EinkSongMenu(
                                        originalSong = song,
                                        playlistSong = playlistSong,
                                        playlistBrowseId = playlist?.playlist?.browseId,
                                        navController = navController,
                                        onDismiss = dismiss,
                                    )
                                },
                                showDivider = index != songs.lastIndex,
                            )
                        }
                    }
                }
            }
        }

        if (songs.isNotEmpty() && !isInEditMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                FloatingActionButtonMMD(
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = playlistName,
                                items = songs.shuffled().map { it.song.toMediaItem() },
                            ),
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle playlist",
                    )
                }
                FloatingActionButtonMMD(
                    onClick = { navController.navigate(einkPlaylistAddSongsRoute(playlistId)) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add songs",
                    )
                }
            }
        }
    }
}

@Composable
fun EinkPlaylistEditScreen(
    navController: NavController,
    playlistId: String,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (songs.isEmpty()) {
                EinkEmptyState(
                    title = "No songs to edit",
                    body = "Add songs to this playlist first.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumnMMD(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, playlistSong -> playlistSong.map.id },
                    ) { index, playlistSong ->
                        val previousPosition = songs.getOrNull(index - 1)?.map?.position
                        val nextPosition = songs.getOrNull(index + 1)?.map?.position
                        EinkEditableSongRow(
                            title = playlistSong.song.song.title,
                            subtitle = songSubtitle(playlistSong.song),
                            canMoveUp = index > 0,
                            canMoveDown = index < songs.lastIndex,
                            onMoveUp = {
                                if (previousPosition != null) {
                                    moveSong(database, viewModel.playlistId, playlistSong.map.position, previousPosition)
                                }
                            },
                            onMoveDown = {
                                if (nextPosition != null) {
                                    moveSong(database, viewModel.playlistId, playlistSong.map.position, nextPosition)
                                }
                            },
                            onRemove = { removeSong(database, coroutineScope, playlistSong.map) },
                            showDivider = index != songs.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EinkPlaylistAddSongsScreen(
    navController: NavController,
    playlistId: String,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val songs by viewModel.allSongs.collectAsState()
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectedCount = selectedIds.count { it.value }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextMMD(
                    text = "$selectedCount selected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                ButtonMMD(
                    onClick = {
                        val ids = selectedIds.filterValues { it }.keys.toList()
                        if (ids.isNotEmpty()) {
                            addSongsToPlaylist(database, coroutineScope, playlistId, ids)
                        }
                        navController.popBackStack()
                    },
                ) {
                    TextMMD(text = "Done")
                }
            }

            if (songs.isEmpty()) {
                EinkEmptyState(
                    title = "No songs available to add",
                    body = "Like or download songs to build your library first.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumnMMD(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        EinkSelectableRow(
                            title = song.song.title,
                            subtitle = songSubtitle(song),
                            checked = selectedIds[song.id] == true,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedIds[song.id] = true
                                } else {
                                    selectedIds.remove(song.id)
                                }
                            },
                            showDivider = index != songs.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

/** Editable playlist row with reorder (up/down) and remove controls. Mirrors CalmTunes. */
@Composable
private fun EinkEditableSongRow(
    title: String,
    subtitle: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TextMMD(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextMMD(
                        text = subtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canMoveUp) {
                    IconButton(onClick = onMoveUp) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = "Move up",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                if (canMoveDown) {
                    IconButton(onClick = onMoveDown) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = "Move down",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove from playlist",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (showDivider) DashedDivider(thickness = 1.dp)
    }
}

private fun deletePlaylists(
    database: MusicDatabase,
    scope: CoroutineScope,
    playlists: List<Playlist>,
) {
    if (playlists.isEmpty()) return
    database.query {
        playlists.forEach { delete(it.playlist) }
    }
    scope.launch(Dispatchers.IO) {
        playlists.forEach { playlist ->
            playlist.playlist.browseId?.let { runCatching { YouTube.deletePlaylist(it) } }
        }
    }
}

private fun moveSong(
    database: MusicDatabase,
    playlistId: String,
    fromPosition: Int,
    toPosition: Int,
) {
    if (fromPosition == toPosition) return
    database.transaction {
        move(playlistId, fromPosition, toPosition)
    }
}

private fun removeSong(
    database: MusicDatabase,
    scope: CoroutineScope,
    map: PlaylistSongMap,
) {
    scope.launch(Dispatchers.IO) {
        database.withTransaction {
            move(map.playlistId, map.position, Int.MAX_VALUE)
            delete(map.copy(position = Int.MAX_VALUE))
        }
    }
}

private fun addSongsToPlaylist(
    database: MusicDatabase,
    scope: CoroutineScope,
    playlistId: String,
    songIds: List<String>,
) {
    scope.launch(Dispatchers.IO) {
        database.withTransaction {
            val playlist = getPlaylistById(playlistId) ?: return@withTransaction
            val existing = playlistDuplicates(playlistId, songIds).toSet()
            val toAdd = songIds.filter { it !in existing }
            if (toAdd.isNotEmpty()) {
                addSongToPlaylist(playlist, toAdd)
            }
        }
    }
}
