/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.search_bar.SearchBarDefaultsMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.flow.MutableStateFlow
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.components.DashedDivider
import moe.rukamori.archivetune.eink.components.EinkBottomSheetMenu
import moe.rukamori.archivetune.eink.components.EinkBottomSheetPage
import moe.rukamori.archivetune.eink.components.EinkBottomSheetPageState
import moe.rukamori.archivetune.eink.components.EinkMenuState
import moe.rukamori.archivetune.eink.components.LocalEinkBottomSheetPageState
import moe.rukamori.archivetune.eink.components.LocalEinkMenuState
import moe.rukamori.archivetune.eink.screens.EinkAlbumDetailsScreen
import moe.rukamori.archivetune.eink.screens.EinkAlbumsScreen
import moe.rukamori.archivetune.eink.screens.EinkArtistDetailsScreen
import moe.rukamori.archivetune.eink.screens.EinkArtistsScreen
import moe.rukamori.archivetune.eink.screens.EinkMoreScreen
import moe.rukamori.archivetune.eink.screens.EinkNowPlayingScreen
import moe.rukamori.archivetune.eink.screens.EinkPlaylistAddSongsScreen
import moe.rukamori.archivetune.eink.screens.EinkPlaylistDetailsScreen
import moe.rukamori.archivetune.eink.screens.EinkPlaylistEditScreen
import moe.rukamori.archivetune.eink.screens.EinkPlaylistsScreen
import moe.rukamori.archivetune.eink.screens.EinkSearchScreen
import moe.rukamori.archivetune.eink.screens.EinkSettingsScreen
import moe.rukamori.archivetune.eink.screens.EinkSongsScreen
import moe.rukamori.archivetune.eink.viewmodels.EinkSearchViewModel
import moe.rukamori.archivetune.ui.screens.LOGIN_ROUTE
import moe.rukamori.archivetune.ui.screens.LOGIN_URL_ARGUMENT
import moe.rukamori.archivetune.ui.screens.LoginScreen
import moe.rukamori.archivetune.viewmodels.LibraryPlaylistsViewModel

/** Route helpers for destinations that take an id argument. */
private fun detailRoute(base: String, argName: String) = "$base/{$argName}"

fun einkAlbumDetailsRoute(albumId: String) = "${EinkScreen.AlbumDetails.route}/$albumId"
fun einkArtistDetailsRoute(artistId: String) = "${EinkScreen.ArtistDetails.route}/$artistId"
fun einkPlaylistDetailsRoute(playlistId: String) = "${EinkScreen.PlaylistDetails.route}/$playlistId"
fun einkPlaylistEditRoute(playlistId: String) = "${EinkScreen.PlaylistEdit.route}/$playlistId"
fun einkPlaylistAddSongsRoute(playlistId: String) = "${EinkScreen.PlaylistAddSongs.route}/$playlistId"

@Composable
fun EinkApp() {
    val searchViewModel: EinkSearchViewModel = viewModel()
    val keyboardController = LocalSoftwareKeyboardController.current

    val navController = rememberNavController()
    val menuState = remember { EinkMenuState() }
    val bottomSheetPageState = remember { EinkBottomSheetPageState() }

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()

    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val libraryPlaylists by playlistsViewModel.allPlaylists.collectAsState()
    val hasLibraryPlaylists = libraryPlaylists.isNotEmpty()

    var isPlaylistsEditMode by remember { mutableStateOf(false) }
    var playlistEditSelectionCount by remember { mutableIntStateOf(0) }
    val playlistEditSelectionIds = remember { mutableSetOf<String>() }
    var showDeletePlaylistsConfirmation by remember { mutableStateOf(false) }

    var isPlaylistDetailsEditMode by remember { mutableStateOf(false) }
    var playlistDetailsSelectionCount by remember { mutableIntStateOf(0) }
    val playlistDetailsSelectionIds = remember { mutableSetOf<String>() }
    var showDeletePlaylistSongsConfirmation by remember { mutableStateOf(false) }
    var isPlaylistDetailsMenuExpanded by remember { mutableStateOf(false) }
    
    var showRenamePlaylistDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalEinkMenuState provides menuState,
        LocalEinkBottomSheetPageState provides bottomSheetPageState,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route
        val topLevelRoutes = remember { einkNavItems.map { it.route } }
        val isTopLevel = currentRoute in topLevelRoutes
        val hasNowPlaying = mediaMetadata != null

        val isOnPlaylistDetails = currentRoute == EinkScreen.PlaylistDetails.route ||
            currentRoute?.startsWith("${EinkScreen.PlaylistDetails.route}/") == true

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                topBar = {
                    Column {
                        EinkTopAppBar(
                            currentDestination = currentDestination,
                            canNavigateBack = !isTopLevel,
                            searchQuery = searchViewModel.query,
                            onSearchQueryChange = searchViewModel::updateQuery,
                            onPerformSearchClick = { searchViewModel.runSearch { keyboardController?.hide() } },
                            isPlaylistsEditMode = isPlaylistsEditMode,
                            isPlaylistDetailsEditMode = isPlaylistDetailsEditMode,
                            playlistEditSelectionCount = playlistEditSelectionCount,
                            playlistDetailsSelectionCount = playlistDetailsSelectionCount,
                            isPlaylistDetailsMenuExpanded = isPlaylistDetailsMenuExpanded,
                            hasNowPlaying = hasNowPlaying,
                            hasLibraryPlaylists = hasLibraryPlaylists,
                            onBackClick = { navController.navigateUp() },
                            onCancelPlaylistsEditClick = { 
                                isPlaylistsEditMode = false 
                                playlistEditSelectionIds.clear()
                                playlistEditSelectionCount = 0
                            },
                            onCancelPlaylistDetailsEditClick = { 
                                isPlaylistDetailsEditMode = false 
                                playlistDetailsSelectionIds.clear()
                                playlistDetailsSelectionCount = 0
                            },
                            onEnterPlaylistsEditClick = { isPlaylistsEditMode = true },
                            onNavigateToSearchClick = { navController.navigate(EinkScreen.Search.route) },
                            onPlaylistDetailsMenuToggle = { isPlaylistDetailsMenuExpanded = !isPlaylistDetailsMenuExpanded },
                            onPlaylistDetailsEditClick = { 
                                isPlaylistDetailsMenuExpanded = false
                                val playlistId = navBackStackEntry?.arguments?.getString("playlistId")
                                if (playlistId != null) {
                                    navController.navigate(einkPlaylistEditRoute(playlistId))
                                }
                            },
                            onPlaylistDetailsAddSongsClick = { 
                                isPlaylistDetailsMenuExpanded = false
                                val playlistId = navBackStackEntry?.arguments?.getString("playlistId")
                                if (playlistId != null) {
                                    navController.navigate(einkPlaylistAddSongsRoute(playlistId))
                                }
                            },
                            onPlaylistDetailsRenameClick = { 
                                isPlaylistDetailsMenuExpanded = false
                                showRenamePlaylistDialog = true
                            },
                            onPlaylistDetailsDeleteClick = { 
                                isPlaylistDetailsMenuExpanded = false
                                showDeletePlaylistSongsConfirmation = true 
                            },
                            onShowDeletePlaylistSongsConfirmationClick = { showDeletePlaylistSongsConfirmation = true },
                            onShowDeletePlaylistsConfirmationClick = { showDeletePlaylistsConfirmation = true },
                            onPlaylistAddSongsDoneClick = { navController.popBackStack() },
                            onNowPlayingClick = { navController.navigate(EinkScreen.NowPlaying.route) },
                        )
                        com.mudita.mmd.components.divider.HorizontalDividerMMD(thickness = 2.dp)
                    }
                },
                bottomBar = {
                    EinkBottomBar(
                        currentDestination = currentDestination,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = EinkScreen.Songs.route,
                    ) {
                        composable(EinkScreen.Songs.route) { EinkSongsScreen(navController) }
                        composable(EinkScreen.Playlists.route) { 
                            EinkPlaylistsScreen(
                                navController = navController,
                                isInEditMode = isPlaylistsEditMode,
                                onSelectionChanged = { 
                                    playlistEditSelectionIds.clear()
                                    playlistEditSelectionIds.addAll(it)
                                    playlistEditSelectionCount = it.size 
                                },
                                showDeleteConfirmation = showDeletePlaylistsConfirmation,
                                onDeleteConfirmed = { 
                                    showDeletePlaylistsConfirmation = false
                                    isPlaylistsEditMode = false
                                    playlistEditSelectionIds.clear()
                                    playlistEditSelectionCount = 0
                                },
                                onCancelDelete = { showDeletePlaylistsConfirmation = false },
                                selectedIds = playlistEditSelectionIds
                            ) 
                        }
                        composable(EinkScreen.Artists.route) { EinkArtistsScreen(navController) }
                        composable(EinkScreen.Albums.route) { EinkAlbumsScreen(navController) }
                        composable(EinkScreen.More.route) { EinkMoreScreen(navController) }
                        composable(EinkScreen.Settings.route) { EinkSettingsScreen(navController) }
                        composable(EinkScreen.Search.route) { EinkSearchScreen(navController, searchViewModel) }
                        composable(EinkScreen.NowPlaying.route) { EinkNowPlayingScreen(navController) }
                        composable(EinkScreen.Downloads.route) { moe.rukamori.archivetune.eink.screens.EinkDownloadsScreen(navController) }

                        composable(
                            route = "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT={$LOGIN_URL_ARGUMENT}",
                            arguments = listOf(
                                navArgument(LOGIN_URL_ARGUMENT) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            LoginScreen(
                                navController = navController,
                                startUrl = backStackEntry.arguments?.getString(LOGIN_URL_ARGUMENT),
                            )
                        }

                        composable(
                            route = detailRoute(EinkScreen.AlbumDetails.route, "albumId"),
                            arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
                        ) { entry ->
                            EinkAlbumDetailsScreen(navController, entry.arguments?.getString("albumId").orEmpty())
                        }
                        composable(
                            route = detailRoute(EinkScreen.ArtistDetails.route, "artistId"),
                            arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
                        ) { entry ->
                            EinkArtistDetailsScreen(navController, entry.arguments?.getString("artistId").orEmpty())
                        }
                        composable(
                            route = detailRoute(EinkScreen.PlaylistDetails.route, "playlistId"),
                            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                        ) { entry ->
                            EinkPlaylistDetailsScreen(
                                navController = navController, 
                                playlistId = entry.arguments?.getString("playlistId").orEmpty(),
                                showRenameDialog = showRenamePlaylistDialog,
                                onRenameDialogDismiss = { showRenamePlaylistDialog = false },
                                showDeleteSheet = showDeletePlaylistSongsConfirmation, // Actually for deleting playlist itself here
                                onDeleteSheetDismiss = { showDeletePlaylistSongsConfirmation = false },
                                isInEditMode = isPlaylistDetailsEditMode,
                                onSelectionChanged = {
                                    playlistDetailsSelectionIds.clear()
                                    playlistDetailsSelectionIds.addAll(it)
                                    playlistDetailsSelectionCount = it.size
                                },
                                selectedIds = playlistDetailsSelectionIds
                            )
                        }
                        composable(
                            route = detailRoute(EinkScreen.PlaylistEdit.route, "playlistId"),
                            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                        ) { entry ->
                            EinkPlaylistEditScreen(navController, entry.arguments?.getString("playlistId").orEmpty())
                        }
                        composable(
                            route = detailRoute(EinkScreen.PlaylistAddSongs.route, "playlistId"),
                            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                        ) { entry ->
                            EinkPlaylistAddSongsScreen(navController, entry.arguments?.getString("playlistId").orEmpty())
                        }
                    }
                }
            }

            EinkBottomSheetMenu(
                state = LocalEinkMenuState.current,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            EinkBottomSheetPage(
                state = LocalEinkBottomSheetPageState.current,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
