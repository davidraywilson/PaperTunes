/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.papertunes.eink.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Download
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import moe.rukamori.papertunes.viewmodels.AlbumUiState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.papertunes.eink.components.EinkNowPlayingButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.paperapps.paperui.components.PaperLazyColumn
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.papertunes.LocalPlayerConnection
import moe.rukamori.papertunes.eink.components.EinkEmptyState
import moe.rukamori.papertunes.eink.components.EinkSongRow
import moe.rukamori.papertunes.eink.components.EinkTwoLineRow
import moe.rukamori.papertunes.eink.components.albumSubtitle
import moe.rukamori.papertunes.eink.components.artistSubtitle
import moe.rukamori.papertunes.eink.einkAlbumDetailsRoute
import moe.rukamori.papertunes.eink.einkArtistDetailsRoute
import moe.rukamori.papertunes.extensions.toMediaItem
import moe.rukamori.papertunes.extensions.togglePlayPause
import moe.rukamori.papertunes.playback.queues.ListQueue
import moe.rukamori.papertunes.eink.components.LocalEinkMenuState
import moe.rukamori.papertunes.eink.menus.EinkSongMenu
import moe.rukamori.papertunes.viewmodels.AlbumViewModel
import moe.rukamori.papertunes.viewmodels.ArtistAlbumsViewModel
import moe.rukamori.papertunes.viewmodels.ArtistSongsViewModel
import moe.rukamori.papertunes.eink.viewmodels.EinkLocalAlbumsViewModel // CUSTOM: E-Ink Local Artists/Albums
import moe.rukamori.papertunes.eink.viewmodels.EinkLocalArtistsViewModel // CUSTOM: E-Ink Local Artists/Albums
import moe.rukamori.papertunes.eink.einkYouTubeArtistDetailsRoute
import androidx.compose.material.icons.outlined.CheckCircle

@Composable
fun EinkArtistsScreen(
    navController: NavController,
    viewModel: EinkLocalArtistsViewModel = hiltViewModel(), // CUSTOM: E-Ink Local Artists/Albums
) {
    val artists by viewModel.allArtists.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {
        if (artists.isEmpty()) {
            EinkEmptyState(
                title = "No artists yet",
                body = "Like or download songs so their artists appear here.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PaperLazyColumn(modifier = Modifier.fillMaxSize(), refreshKey = artists) {
                itemsIndexed(
                    items = artists,
                    key = { _, artist -> artist.id },
                ) { index, artist ->
                    EinkTwoLineRow(
                        title = artist.artist.name,
                        subtitle = artistSubtitle(artist),
                        onClick = {
                            if (artist.artist.channelId != null) {
                                navController.navigate(einkYouTubeArtistDetailsRoute(artist.artist.channelId!!))
                            } else {
                                navController.navigate(einkArtistDetailsRoute(artist.id))
                            }
                        },
                        showDivider = index != artists.lastIndex,
                        subtitleTrailingIcon = if (artist.artist.channelId != null) Icons.Outlined.CheckCircle else null,
                    )
                }
            }
        }
    }
}

@Composable
fun EinkAlbumsScreen(
    navController: NavController,
    viewModel: EinkLocalAlbumsViewModel = hiltViewModel(), // CUSTOM: E-Ink Local Artists/Albums
) {
    val albums by viewModel.allAlbums.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {
        if (albums.isEmpty()) {
            EinkEmptyState(
                title = "No albums yet",
                body = "Like or download songs so their albums appear here.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PaperLazyColumn(modifier = Modifier.fillMaxSize(), refreshKey = albums) {
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

    Column(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {


        com.paperapps.paperui.components.PanoramaHeader(
            pagerState = pagerState,
            titles = tabOptions,
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            screenTitle = artistName.takeIf { it.isNotBlank() }
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
                    PaperLazyColumn(modifier = Modifier.fillMaxSize(), refreshKey = songs) {
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
                    PaperLazyColumn(modifier = Modifier.fillMaxSize(), refreshKey = albums) {
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
                    onClick = { navController.navigate(moe.rukamori.papertunes.eink.EinkScreen.Search.route) }
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
                            navController.navigate(moe.rukamori.papertunes.eink.EinkScreen.NowPlaying.route)
                        }
                    }
                )
            ),
            menuItems = emptyList(),
            leftSlot = { EinkNowPlayingButton(navController) },
            pagerState = pagerState,
            onBack = { navController.navigateUp() }
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
    val isLocal = album?.album?.isLocal ?: false

    val context = androidx.compose.ui.platform.LocalContext.current
    val downloadUtil = moe.rukamori.papertunes.LocalDownloadUtil.current
    val downloadsMap by downloadUtil.downloads.collectAsStateWithLifecycle()

    val downloadState = remember(songs, downloadsMap) {
        val songIds = songs.filter { !it.song.isLocal }.map { it.id }
        moe.rukamori.papertunes.ui.utils.headerDownloadState(songIds, downloadsMap)
    }
    
    val hasUndownloadedSongs = downloadState != moe.rukamori.papertunes.ui.utils.HeaderDownloadState.Completed

    val tabOptions = remember { listOf("Songs", "Details") }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabOptions.size })
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {


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
                    coroutineScope = coroutineScope,
                    modifier = Modifier.fillMaxWidth(),
                    screenTitle = albumTitle.takeIf { it.isNotBlank() }
                )

                Box(modifier = Modifier.weight(1f)) {
                    com.paperapps.paperui.components.PanoramaPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                
                                if (uiState is AlbumUiState.Empty || (uiState is AlbumUiState.Content && songs.isEmpty())) {
                                    EinkEmptyState(
                                        title = albumTitle.ifBlank { "Album" },
                                        body = "No songs in this album yet.",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    PaperLazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        refreshKey = songs,
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
                            PaperLazyColumn(modifier = Modifier.fillMaxSize(), refreshKey = album) {
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

                }
            }
        }

        com.paperapps.paperui.components.ApplicationBar(
            actions = buildList {
                val artist = album?.artists?.firstOrNull()
                if (artist != null) {
                    add(
                        com.paperapps.paperui.components.AppbarAction(
                            icon = Icons.Outlined.Person,
                            label = "Artist",
                            onClick = {
                                if (artist.channelId != null) {
                                    navController.navigate(moe.rukamori.papertunes.eink.einkYouTubeArtistDetailsRoute(artist.channelId))
                                } else {
                                    navController.navigate(moe.rukamori.papertunes.eink.einkArtistDetailsRoute(artist.id))
                                }
                            }
                        )
                    )
                }

                add(
                    com.paperapps.paperui.components.AppbarAction(
                        icon = Icons.Outlined.Shuffle,
                        label = "Shuffle",
                        onClick = {
                            if (songs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = albumTitle.ifBlank { "Album" },
                                        items = songs.shuffled().map { it.toMediaItem() },
                                    ),
                                )
                                navController.navigate(moe.rukamori.papertunes.eink.EinkScreen.NowPlaying.route)
                            }
                        }
                    )
                )

                if (!isLocal && songs.isNotEmpty() && hasUndownloadedSongs) {
                    add(
                        com.paperapps.paperui.components.AppbarAction(
                            icon = Icons.Outlined.Download,
                            label = "Download",
                            onClick = {
                                moe.rukamori.papertunes.ui.utils.sendAddMissingDownloads(
                                    context = context,
                                    songs = songs.map { 
                                        moe.rukamori.papertunes.ui.utils.HeaderDownloadItem(
                                            id = it.id,
                                            title = it.song.title,
                                        ) 
                                    },
                                    downloads = downloadsMap,
                                )
                            }
                        )
                    )
                }
            },
            menuItems = emptyList(),
            leftSlot = { moe.rukamori.papertunes.eink.components.EinkNowPlayingButton(navController) },
            pagerState = pagerState,
            onBack = { navController.navigateUp() }
        )
    }
}
