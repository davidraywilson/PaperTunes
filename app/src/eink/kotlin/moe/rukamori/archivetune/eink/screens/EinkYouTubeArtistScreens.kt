/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.db.entities.ArtistEntity
import moe.rukamori.archivetune.eink.components.DashedDivider
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkSongRow
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow
import moe.rukamori.archivetune.eink.components.albumSubtitle
import moe.rukamori.archivetune.eink.einkAlbumDetailsRoute
import moe.rukamori.archivetune.eink.einkYouTubeArtistDetailsRoute
import moe.rukamori.archivetune.eink.menus.EinkSongMenu
import moe.rukamori.archivetune.eink.menus.EinkYouTubeSongMenu
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.innertube.pages.ArtistPage
import moe.rukamori.archivetune.innertube.pages.ArtistSectionLayout
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.playback.queues.YouTubeQueue
import moe.rukamori.archivetune.viewmodels.ArtistViewModel

private const val TAB_LIBRARY = 0
private const val TAB_ONLINE = 1

/**
 * E-ink version of the YouTube Music artist detail page.
 *
 * Mirrors the functionality of [moe.rukamori.archivetune.ui.screens.artist.ArtistScreen]
 * but uses only MMD/e-ink components:
 *  - Artist name + song/album count displayed in the top app bar (via EinkChrome)
 *  - Library / Online tabs (PrimaryTabRowMMD) at the top of the screen
 *  - Subscribe / Shuffle / Radio action buttons
 *  - Expandable artist description (online tab only)
 *  - Local library sections (songs + albums) and YouTube online sections
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EinkYouTubeArtistScreen(
    navController: NavController,
    artistId: String,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current

    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsStateWithLifecycle()
    val librarySongs by viewModel.librarySongs.collectAsStateWithLifecycle()
    val libraryAlbums by viewModel.libraryAlbums.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val hasLibraryContent = librarySongs.isNotEmpty() || libraryAlbums.isNotEmpty()
    // Default to Library tab when artist has local content; Online otherwise.
    var selectedTab by rememberSaveable {
        mutableIntStateOf(if (librarySongs.isNotEmpty()) TAB_LIBRARY else TAB_ONLINE)
    }

    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: "Artist"
    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Tabs ─────────────────────────────────────────────────────────────────────────
            // Always show tabs so the user can switch to online even if library is empty.
            PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
                TabMMD(
                    selected = selectedTab == TAB_LIBRARY,
                    onClick = { selectedTab = TAB_LIBRARY },
                    text = {
                        TextMMD(
                            text = "Library",
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == TAB_LIBRARY) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
                TabMMD(
                    selected = selectedTab == TAB_ONLINE,
                    onClick = {
                        selectedTab = TAB_ONLINE
                        if (artistPage == null) viewModel.fetchArtistsFromYTM()
                    },
                    text = {
                        TextMMD(
                            text = "Online",
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == TAB_ONLINE) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }

            // ── Content ───────────────────────────────────────────────────────────────────────
            LazyColumnMMD(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {

            // ── Library tab ───────────────────────────────────────────────────────────────
            if (selectedTab == TAB_LIBRARY) {
                if (librarySongs.isNotEmpty()) {
                    item(key = "local-songs-header") {
                        TextMMD(
                            text = "Songs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    itemsIndexed(
                        items = librarySongs,
                        key = { _, song -> "local_song_${song.id}" },
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
                                            title = artistName,
                                            items = librarySongs.map { it.toMediaItem() },
                                            startIndex = index,
                                        )
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
                            showDivider = index != librarySongs.lastIndex || libraryAlbums.isNotEmpty(),
                        )
                    }
                }

                if (libraryAlbums.isNotEmpty()) {
                    item(key = "local-albums-header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextMMD(
                            text = "Albums",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    itemsIndexed(
                        items = libraryAlbums,
                        key = { _, album -> "local_album_${album.id}" },
                    ) { index, album ->
                        EinkTwoLineRow(
                            title = album.album.title,
                            subtitle = albumSubtitle(album),
                            onClick = { navController.navigate(einkAlbumDetailsRoute(album.id)) },
                            showDivider = index != libraryAlbums.lastIndex,
                        )
                    }
                }

                if (!hasLibraryContent) {
                    item(key = "local-empty") {
                        EinkEmptyState(
                            title = "Nothing in library",
                            body = "Like or download this artist's songs to see them here.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

            // ── Online tab ────────────────────────────────────────────────────────────────
            } else {
                // Expandable description
                val description = artistPage?.description
                if (!description.isNullOrBlank()) {
                    item(key = "artist-description") {
                        EinkExpandableDescription(description = description)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                val excludedSections = listOf("live performances", "from your library", "featured on")
                artistPage?.sections?.filterNot { section ->
                    val title = section.title.lowercase()
                    title in excludedSections || title.contains("playlist")
                }?.forEach { section ->
                    if (section.items.isEmpty()) return@forEach

                    item(
                        key = "yt_header_${section.title}_${section.items.firstOrNull()?.id.orEmpty()}",
                    ) {
                        TextMMD(
                            text = section.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    if (section.layout == ArtistSectionLayout.LIST &&
                        section.items.all { it is SongItem }
                    ) {
                        val songItems = section.items.distinctBy { it.id }
                        itemsIndexed(
                            items = songItems,
                            key = { _, item -> "yt_song_${item.id}" },
                        ) { index, item ->
                            val song = item as SongItem
                            EinkArtistYouTubeSongRow(
                                song = song,
                                isCurrentlyPlaying = song.id == mediaMetadata?.id,
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = song.id),
                                                song.toMediaMetadata(),
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                dropdownContent = { dismiss ->
                                    EinkYouTubeSongMenu(
                                        song = song,
                                        navController = navController,
                                        onDismiss = dismiss,
                                    )
                                },
                                showDivider = index != songItems.lastIndex,
                            )
                        }
                    } else {
                        val gridItems = section.items.distinctBy { it.id }
                        itemsIndexed(
                            items = gridItems,
                            key = { _, item ->
                                val type = when (item) {
                                    is SongItem -> "song"
                                    is AlbumItem -> "album"
                                    is ArtistItem -> "artist"
                                    is PlaylistItem -> "playlist"
                                    else -> "item"
                                }
                                "yt_${type}_${item.id}"
                            },
                        ) { index, item ->
                            val title = when (item) {
                                is SongItem -> item.title
                                is AlbumItem -> item.title
                                is ArtistItem -> item.title
                                is PlaylistItem -> item.title
                                else -> ""
                            }
                            val subtitle = when (item) {
                                is AlbumItem -> buildString {
                                    item.artists?.joinToString(", ") { it.name }
                                        ?.takeIf { it.isNotBlank() }?.let { append(it) }
                                    item.year?.let { y ->
                                        if (isNotEmpty()) append(" • ")
                                        append(y)
                                    }
                                }.takeIf { it.isNotBlank() }
                                is ArtistItem -> item.subscriberCountText
                                is PlaylistItem -> item.songCountText
                                is SongItem -> item.artists.joinToString(", ") { it.name }
                                    .takeIf { it.isNotBlank() }
                                else -> null
                            }
                            EinkTwoLineRow(
                                title = title,
                                subtitle = subtitle,
                                onClick = {
                                    when (item) {
                                        is SongItem -> playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata(),
                                            )
                                        )
                                        is AlbumItem -> navController.navigate("album/${item.id}")
                                        is ArtistItem -> navController.navigate(einkYouTubeArtistDetailsRoute(item.id))
                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        else -> {}
                                    }
                                },
                                showDivider = index != gridItems.lastIndex,
                            )
                        }
                    }

                    item(key = "yt_spacer_${section.title}") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (artistPage?.sections.isNullOrEmpty()) {
                    item(key = "yt-loading") {
                        EinkEmptyState(
                            title = "Loading…",
                            body = "Fetching artist from YouTube Music.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    val canShuffle = if (selectedTab == TAB_LIBRARY) librarySongs.isNotEmpty()
    else artistPage?.artist?.shuffleEndpoint != null

    if (canShuffle) {
        com.mudita.mmd.components.buttons.FloatingActionButtonMMD(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                if (selectedTab == TAB_LIBRARY) {
                    if (librarySongs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = artistName,
                                items = librarySongs.shuffled().map { it.toMediaItem() },
                            )
                        )
                    }
                } else {
                    artistPage?.artist?.shuffleEndpoint?.let {
                        playerConnection.playQueue(YouTubeQueue(it))
                    }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = "Shuffle",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
}

// ── Private helpers ──────────────────────────────────────────────────────────────────────────────

/**
 * Expandable artist description. Collapses to 4 lines by default; tapping expands/collapses.
 */
@Composable
private fun EinkExpandableDescription(description: String) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(bottom = 8.dp),
    ) {
        TextMMD(
            text = description,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (!isExpanded) {
            TextMMD(
                text = "More",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Song row for online [SongItem] entries within the artist screen. Consistent with the e-ink
 * design language used throughout [EinkSearchScreen] and [EinkArtistAlbumScreens].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EinkArtistYouTubeSongRow(
    song: SongItem,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    dropdownContent: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    showDivider: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (dropdownContent != null) expanded = true
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
                        modifier = Modifier.padding(start = 4.dp),
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
                    val subtitle = buildString {
                        val artist = song.artists.joinToString(", ") { it.name }
                        if (artist.isNotBlank()) append(artist)
                        song.album?.name?.takeIf { it.isNotBlank() }?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it)
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        TextMMD(
                            text = subtitle,
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
                offset = DpOffset(x = 16.dp, y = 0.dp),
            ) {
                dropdownContent { expanded = false }
            }
        }
    }
}
