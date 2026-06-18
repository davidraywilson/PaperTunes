package moe.rukamori.archivetune.eink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.search_bar.SearchBarDefaultsMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import moe.rukamori.archivetune.eink.components.DashedDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkTopAppBar(
    currentDestination: NavDestination?,
    canNavigateBack: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPerformSearchClick: () -> Unit,
    isPlaylistsEditMode: Boolean,
    isPlaylistDetailsEditMode: Boolean,
    playlistEditSelectionCount: Int,
    playlistDetailsSelectionCount: Int,
    isPlaylistDetailsMenuExpanded: Boolean,
    hasNowPlaying: Boolean,
    hasLibraryPlaylists: Boolean,
    selectedPlaylistName: String? = null,
    onBackClick: () -> Unit,
    onCancelPlaylistsEditClick: () -> Unit,
    onCancelPlaylistDetailsEditClick: () -> Unit,
    onEnterPlaylistsEditClick: () -> Unit,
    onNavigateToSearchClick: () -> Unit,
    onPlaylistDetailsMenuToggle: () -> Unit,
    onPlaylistDetailsEditClick: () -> Unit,
    onPlaylistDetailsAddSongsClick: () -> Unit,
    onPlaylistDetailsRenameClick: () -> Unit,
    onPlaylistDetailsDeleteClick: () -> Unit,
    onPlaylistDetailsDownloadClick: () -> Unit,
    onShowDeletePlaylistSongsConfirmationClick: () -> Unit,
    onShowDeletePlaylistsConfirmationClick: () -> Unit,
    onPlaylistAddSongsDoneClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navRoutes = remember { einkNavItems.map { it.route } }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isOnPlaylistDetails = currentDestination?.route == EinkScreen.PlaylistDetails.route ||
        currentDestination?.route?.startsWith("${EinkScreen.PlaylistDetails.route}/") == true

    TopAppBarMMD(
        navigationIcon = {
            when {
                isOnPlaylistDetails && isPlaylistDetailsEditMode -> {
                    IconButton(onClick = onCancelPlaylistDetailsEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Cancel playlist edits",
                        )
                    }
                }

                currentDestination?.route == EinkScreen.Playlists.route && isPlaylistsEditMode -> {
                    IconButton(onClick = onCancelPlaylistsEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Cancel playlist edit",
                        )
                    }
                }

                canNavigateBack && currentDestination?.route !in navRoutes -> {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            }
        },
        title = {
            when {
                currentDestination?.route == EinkScreen.Search.route -> {
                    SearchBarDefaultsMMD.InputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearch = {
                            keyboardController?.hide()
                            onPerformSearchClick()
                        },
                        expanded = true,
                        onExpandedChange = { },
                        placeholder = { TextMMD("Search") },
                        trailingIcon = {
                            Row {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Clear,
                                            contentDescription = "Clear search",
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        onPerformSearchClick()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                    )
                                }
                            }
                        },
                    )
                }
                isOnPlaylistDetails && selectedPlaylistName != null -> {
                    Text(
                        text = selectedPlaylistName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                else -> {
                    Text(
                        text = einkAppBarTitle(currentDestination),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            EinkTopAppBarActions(
                currentDestination = currentDestination,
                isOnPlaylistDetails = isOnPlaylistDetails,
                isPlaylistsEditMode = isPlaylistsEditMode,
                playlistEditSelectionCount = playlistEditSelectionCount,
                playlistDetailsSelectionCount = playlistDetailsSelectionCount,
                isPlaylistDetailsEditMode = isPlaylistDetailsEditMode,
                isPlaylistDetailsMenuExpanded = isPlaylistDetailsMenuExpanded,
                hasLibraryPlaylists = hasLibraryPlaylists,
                hasNowPlaying = hasNowPlaying,
                onEnterPlaylistsEditClick = onEnterPlaylistsEditClick,
                onNavigateToSearchClick = onNavigateToSearchClick,
                onPlaylistDetailsMenuToggle = onPlaylistDetailsMenuToggle,
                onPlaylistDetailsEditClick = onPlaylistDetailsEditClick,
                onPlaylistDetailsAddSongsClick = onPlaylistDetailsAddSongsClick,
                onPlaylistDetailsRenameClick = onPlaylistDetailsRenameClick,
                onPlaylistDetailsDeleteClick = onPlaylistDetailsDeleteClick,
                onPlaylistDetailsDownloadClick = onPlaylistDetailsDownloadClick,
                onShowDeletePlaylistSongsConfirmationClick = onShowDeletePlaylistSongsConfirmationClick,
                onShowDeletePlaylistsConfirmationClick = onShowDeletePlaylistsConfirmationClick,
                onPlaylistAddSongsDoneClick = onPlaylistAddSongsDoneClick,
                onNowPlayingClick = onNowPlayingClick,
            )
        },
        showDivider = false,
        modifier = modifier,
    )
}

@Composable
private fun EinkTopAppBarActions(
    currentDestination: NavDestination?,
    isOnPlaylistDetails: Boolean,
    isPlaylistsEditMode: Boolean,
    playlistEditSelectionCount: Int,
    playlistDetailsSelectionCount: Int,
    isPlaylistDetailsEditMode: Boolean,
    isPlaylistDetailsMenuExpanded: Boolean,
    hasLibraryPlaylists: Boolean,
    hasNowPlaying: Boolean,
    onEnterPlaylistsEditClick: () -> Unit,
    onNavigateToSearchClick: () -> Unit,
    onPlaylistDetailsMenuToggle: () -> Unit,
    onPlaylistDetailsEditClick: () -> Unit,
    onPlaylistDetailsAddSongsClick: () -> Unit,
    onPlaylistDetailsRenameClick: () -> Unit,
    onPlaylistDetailsDeleteClick: () -> Unit,
    onPlaylistDetailsDownloadClick: () -> Unit,
    onShowDeletePlaylistSongsConfirmationClick: () -> Unit,
    onShowDeletePlaylistsConfirmationClick: () -> Unit,
    onPlaylistAddSongsDoneClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
) {
    val navRoutes = remember { einkNavItems.map { it.route } }

    if (currentDestination?.route != EinkScreen.Search.route && currentDestination?.route in navRoutes) {
        if (currentDestination?.route == EinkScreen.Playlists.route && hasLibraryPlaylists && !isPlaylistsEditMode) {
            IconButton(onClick = onEnterPlaylistsEditClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit playlists",
                )
            }
        }

        IconButton(onClick = onNavigateToSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
            )
        }
    }

    if (isOnPlaylistDetails && !isPlaylistDetailsEditMode) {
        Box {
            IconButton(onClick = onPlaylistDetailsMenuToggle) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Playlist options",
                )
            }

            DropdownMenuMMD(
                expanded = isPlaylistDetailsMenuExpanded,
                onDismissRequest = onPlaylistDetailsMenuToggle,
            ) {
                DropdownMenuItemMMD(
                    text = { TextMMD("Edit") },
                    onClick = onPlaylistDetailsEditClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Add songs") },
                    onClick = onPlaylistDetailsAddSongsClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Rename") },
                    onClick = onPlaylistDetailsRenameClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Delete") },
                    onClick = onPlaylistDetailsDeleteClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Download playlist") },
                    onClick = onPlaylistDetailsDownloadClick,
                )
            }
        }
    }

    if (
        currentDestination?.route == EinkScreen.Playlists.route &&
        isPlaylistsEditMode &&
        playlistEditSelectionCount > 0
    ) {
        OutlinedButtonMMD(
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
            onClick = onShowDeletePlaylistsConfirmationClick,
        ) {
            TextMMD(
                text = "Delete $playlistEditSelectionCount",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (
        isOnPlaylistDetails &&
        isPlaylistDetailsEditMode &&
        playlistDetailsSelectionCount > 0
    ) {
        OutlinedButtonMMD(
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
            onClick = onShowDeletePlaylistSongsConfirmationClick,
        ) {
            TextMMD(
                text = "Remove $playlistDetailsSelectionCount",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (currentDestination?.route?.startsWith(EinkScreen.PlaylistAddSongs.route) == true) {
        ButtonMMD(
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
            onClick = onPlaylistAddSongsDoneClick,
        ) {
            TextMMD(
                text = "Done",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (
        hasNowPlaying &&
        currentDestination?.route?.startsWith(EinkScreen.PlaylistAddSongs.route) != true &&
        currentDestination?.route != EinkScreen.NowPlaying.route &&
        currentDestination?.route != EinkScreen.Search.route &&
        !(currentDestination?.route == EinkScreen.Playlists.route && isPlaylistsEditMode) &&
        !(isOnPlaylistDetails && isPlaylistDetailsEditMode)
    ) {
        ButtonMMD(
            onClick = onNowPlayingClick,
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            TextMMD(
                text = "Now Playing",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun EinkBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
) {
    val navRoutes = remember { einkNavItems.map { it.route } }

    if (currentDestination?.route in navRoutes) {
        NavigationBarMMD(
            modifier = Modifier.padding(bottom = 2.dp),
        ) {
            einkNavItems.forEach { screen ->
                val isSelected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItemMMD(
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                        )
                    },
                    label = {
                        TextMMD(
                            text = screen.label,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        )
                    },
                    selected = isSelected,
                    onClick = { onNavigate(screen.route) },
                )
            }
        }
    }
}
