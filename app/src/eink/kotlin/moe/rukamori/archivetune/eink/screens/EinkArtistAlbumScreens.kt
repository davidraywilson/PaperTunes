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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import moe.rukamori.archivetune.viewmodels.AlbumUiState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.eink.components.EinkNowPlayingButton
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val songs by songsViewModel.songs.collectAsState()
    val artist by songsViewModel.artist.collectAsState()
    val albums by albumsViewModel.albums.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val artistName = artist?.artist?.name.orEmpty()
    val tabOptions = remember { listOf("Songs", "Albums") }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabOptions.size })

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {


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

        com.paperapps.paperui.components.PanoramaHeader(
            pagerState = pagerState,
            titles = tabOptions,
            coroutineScope = coroutineScope
        )

        com.paperapps.paperui.components.PanoramaPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {
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

        com.paperapps.paperui.components.ApplicationBar(
            actions = listOf(
                com.paperapps.paperui.components.AppbarAction(
                    icon = Icons.Outlined.Search,
                    label = "Search",
                    onClick = { navController.navigate(moe.rukamori.archivetune.eink.EinkScreen.Search.route) }
                ),
                com.paperapps.paperui.components.AppbarAction(
                    icon = Icons.Outlined.Shuffle,
                    label = "Shuffle",
                    onClick = { 
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artistName.ifBlank { "Artist" },
                                    items = songs.shuffled().map { it.toMediaItem() },
                                ),
                            )
                            navController.navigate(moe.rukamori.archivetune.eink.EinkScreen.NowPlaying.route)
                        }
                    }
                )
            ),
            menuItems = emptyList(),
            leftSlot = { EinkNowPlayingButton(navController) }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EinkAlbumDetailsScreen(
    navController: NavController,
    albumId: String,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val menuState = LocalEinkMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otherVersions by viewModel.otherVersions.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val album = albumWithSongs
    val songs = album?.songs.orEmpty()
    val albumTitle = album?.album?.title.orEmpty()

    val tabOptions = remember { listOf("Songs", "Details") }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabOptions.size })
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {


        when {
            uiState is AlbumUiState.Loading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                }
            }
            uiState is AlbumUiState.Error -> {
                EinkEmptyState(
                    title = "Error",
                    body = "Failed to fetch album details.",
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {
                com.paperapps.paperui.components.PanoramaHeader(
                    pagerState = pagerState,
                    titles = tabOptions,
                    coroutineScope = coroutineScope
                )

                Box(modifier = Modifier.weight(1f)) {
                    com.paperapps.paperui.components.PanoramaPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (albumTitle.isNotBlank()) {
                                    TextMMD(
                                        text = albumTitle,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                    )
                                }
                                
                                if (uiState is AlbumUiState.Empty || (uiState is AlbumUiState.Content && songs.isEmpty())) {
                                    EinkEmptyState(
                                        title = albumTitle.ifBlank { "Album" },
                                        body = "No songs in this album yet.",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    LazyColumnMMD(
                                        contentPadding = PaddingValues(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
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
                                }
                            }
                        } else {
                            LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                                if (album != null) {
                                    item(key = "album_info") {
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            TextMMD(
                                                text = "Album Information",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            if (album.artists.isNotEmpty()) {
                                                val artistsText = album.artists.joinToString { it.name }
                                                TextMMD(
                                                    text = "Artist(s): $artistsText",
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                            if (album.album.year != null && album.album.year != 0) {
                                                TextMMD(
                                                    text = "Released: ${album.album.year}",
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                            val numTracks = album.songs.size.takeIf { it > 0 } ?: album.album.songCount
                                            if (numTracks > 0) {
                                                TextMMD(
                                                    text = "Tracks: $numTracks",
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (otherVersions.isNotEmpty()) {
                                    item(key = "other_versions_header") {
                                        TextMMD(
                                            text = "Other Versions",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                        )
                                    }
                                    itemsIndexed(
                                        items = otherVersions,
                                        key = { _, version -> "other_version_${version.id}" },
                                    ) { index, version ->
                                        EinkTwoLineRow(
                                            title = version.title,
                                            subtitle = version.year?.toString() ?: "",
                                            onClick = { navController.navigate(einkAlbumDetailsRoute(version.id)) },
                                            showDivider = index != otherVersions.lastIndex,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (songs.isNotEmpty()) {
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
        }

        com.paperapps.paperui.components.ApplicationBar(
            actions = listOf(
                com.paperapps.paperui.components.AppbarAction(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    label = "Back",
                    onClick = { navController.navigateUp() }
                )
            ),
            menuItems = emptyList(),
            leftSlot = { moe.rukamori.archivetune.eink.components.EinkNowPlayingButton(navController) }
        )
    }
}
