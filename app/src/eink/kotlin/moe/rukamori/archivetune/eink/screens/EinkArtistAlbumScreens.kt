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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkSongRow
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow
import moe.rukamori.archivetune.eink.components.albumSubtitle
import moe.rukamori.archivetune.eink.components.artistSubtitle
import moe.rukamori.archivetune.eink.einkAlbumDetailsRoute
import moe.rukamori.archivetune.eink.einkArtistDetailsRoute
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.eink.components.LocalEinkMenuState
import moe.rukamori.archivetune.eink.menus.EinkSongMenu
import moe.rukamori.archivetune.viewmodels.AlbumViewModel
import moe.rukamori.archivetune.viewmodels.ArtistAlbumsViewModel
import moe.rukamori.archivetune.viewmodels.ArtistSongsViewModel
import moe.rukamori.archivetune.viewmodels.LibraryAlbumsViewModel
import moe.rukamori.archivetune.viewmodels.LibraryArtistsViewModel

@Composable
fun EinkArtistsScreen(
    navController: NavController,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val artists by viewModel.allArtists.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (artists.isEmpty()) {
            EinkEmptyState(
                title = "No artists yet",
                body = "Like or download songs so their artists appear here.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                itemsIndexed(
                    items = artists,
                    key = { _, artist -> artist.id },
                ) { index, artist ->
                    EinkTwoLineRow(
                        title = artist.artist.name,
                        subtitle = artistSubtitle(artist),
                        onClick = { navController.navigate(einkArtistDetailsRoute(artist.id)) },
                        showDivider = index != artists.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
fun EinkAlbumsScreen(
    navController: NavController,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
    val albums by viewModel.allAlbums.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (albums.isEmpty()) {
            EinkEmptyState(
                title = "No albums yet",
                body = "Like or download songs so their albums appear here.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                itemsIndexed(
                    items = albums,
                    key = { _, album -> album.id },
                ) { index, album ->
                    EinkTwoLineRow(
                        title = album.album.title,
                        subtitle = albumSubtitle(album),
                        onClick = { navController.navigate(einkAlbumDetailsRoute(album.id)) },
                        showDivider = index != albums.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
fun EinkArtistDetailsScreen(
    navController: NavController,
    artistId: String,
    songsViewModel: ArtistSongsViewModel = hiltViewModel(),
    albumsViewModel: ArtistAlbumsViewModel = hiltViewModel(),
) {
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val songs by songsViewModel.songs.collectAsState()
    val artist by songsViewModel.artist.collectAsState()
    val albums by albumsViewModel.albums.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val artistName = artist?.artist?.name.orEmpty()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabOptions = remember { listOf("Songs", "Albums") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (artistName.isNotBlank()) {
                TextMMD(
                    text = artistName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                )
            }

            PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
                tabOptions.forEachIndexed { index, title ->
                    TabMMD(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            TextMMD(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            if (selectedTab == 0) {
                if (songs.isEmpty()) {
                    EinkEmptyState(
                        title = "No songs",
                        body = "This artist has no songs in your library yet.",
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
                                                title = artistName.ifBlank { "Artist" },
                                                items = songs.map { it.toMediaItem() },
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
                                        navController = navController,
                                        onDismiss = dismiss,
                                    )
                                },
                                showDivider = index != songs.lastIndex,
                            )
                        }
                    }
                }
            } else {
                if (albums.isEmpty()) {
                    EinkEmptyState(
                        title = "No albums",
                        body = "This artist has no albums in your library yet.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                        itemsIndexed(
                            items = albums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            EinkTwoLineRow(
                                title = album.album.title,
                                subtitle = albumSubtitle(album),
                                onClick = { navController.navigate(einkAlbumDetailsRoute(album.id)) },
                                showDivider = index != albums.lastIndex,
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 0 && songs.isNotEmpty()) {
            FloatingActionButtonMMD(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = artistName.ifBlank { "Artist" },
                            items = songs.shuffled().map { it.toMediaItem() },
                        ),
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "Shuffle artist songs",
                )
            }
        }
    }
}

@Composable
fun EinkAlbumDetailsScreen(
    navController: NavController,
    albumId: String,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val album = albumWithSongs
    val songs = album?.songs.orEmpty()
    val albumTitle = album?.album?.title.orEmpty()
    val albumArtist = album?.artists?.joinToString(", ") { it.name }.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (album == null) {
            EinkEmptyState(
                title = "Loading album…",
                body = "Fetching album details.",
                modifier = Modifier.fillMaxSize(),
            )
        } else if (songs.isEmpty()) {
            EinkEmptyState(
                title = albumTitle.ifBlank { "Album" },
                body = "No songs in this album yet.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                item(key = "album-header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextMMD(
                            text = albumTitle,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (albumArtist.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextMMD(
                                text = albumArtist,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id },
                ) { index, song ->
                    EinkSongRow(
                        song = song,
                        isCurrentlyPlaying = song.id == mediaMetadata?.id,
                        trackNumber = index + 1,
                        onClick = {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = albumTitle.ifBlank { "Album" },
                                        items = songs.map { it.toMediaItem() },
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
                                navController = navController,
                                onDismiss = dismiss,
                            )
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
                    playerConnection.playQueue(
                        ListQueue(
                            title = albumTitle.ifBlank { "Album" },
                            items = songs.shuffled().map { it.toMediaItem() },
                        ),
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "Shuffle album",
                )
            }
        }
    }
}
