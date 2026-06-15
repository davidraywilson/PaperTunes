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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.flow.MutableStateFlow
import moe.rukamori.archivetune.LocalPlayerConnection
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
import moe.rukamori.archivetune.ui.component.BottomSheetMenu
import moe.rukamori.archivetune.ui.component.BottomSheetPage
import moe.rukamori.archivetune.ui.component.BottomSheetPageState
import moe.rukamori.archivetune.ui.component.LocalBottomSheetPageState
import moe.rukamori.archivetune.ui.component.LocalMenuState
import moe.rukamori.archivetune.ui.component.MenuState
import androidx.compose.runtime.CompositionLocalProvider

/** Route helpers for destinations that take an id argument. */
private fun detailRoute(base: String, argName: String) = "$base/{$argName}"

fun einkAlbumDetailsRoute(albumId: String) = "${EinkScreen.AlbumDetails.route}/$albumId"
fun einkArtistDetailsRoute(artistId: String) = "${EinkScreen.ArtistDetails.route}/$artistId"
fun einkPlaylistDetailsRoute(playlistId: String) = "${EinkScreen.PlaylistDetails.route}/$playlistId"
fun einkPlaylistEditRoute(playlistId: String) = "${EinkScreen.PlaylistEdit.route}/$playlistId"
fun einkPlaylistAddSongsRoute(playlistId: String) = "${EinkScreen.PlaylistAddSongs.route}/$playlistId"

@Composable
fun EinkApp() {
    val navController = rememberNavController()
    val menuState = remember { MenuState() }
    val bottomSheetPageState = remember { BottomSheetPageState() }

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()

    CompositionLocalProvider(
        LocalMenuState provides menuState,
        LocalBottomSheetPageState provides bottomSheetPageState,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route
        val topLevelRoutes = remember { einkNavItems.map { it.route } }
        val isTopLevel = currentRoute in topLevelRoutes
        val hasNowPlaying = mediaMetadata != null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBarMMD(
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        }
                    },
                    title = {
                        TextMMD(
                            text = einkAppBarTitle(currentDestination),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    },
                    actions = {
                        if (isTopLevel && currentRoute != EinkScreen.More.route) {
                            IconButton(onClick = { navController.navigate(EinkScreen.Search.route) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                )
                            }
                        }
                        if (
                            hasNowPlaying &&
                            currentRoute != EinkScreen.NowPlaying.route &&
                            currentRoute != EinkScreen.Search.route
                        ) {
                            ButtonMMD(
                                onClick = { navController.navigate(EinkScreen.NowPlaying.route) },
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                TextMMD(
                                    text = "Now Playing",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    },
                    showDivider = false,
                )

                Box(modifier = Modifier.weight(1f)) {
                    EinkNavHost(navController)
                }

                if (isTopLevel) {
                    NavigationBarMMD(modifier = Modifier.padding(bottom = 2.dp)) {
                        einkNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
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
                                        fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            BottomSheetMenu(
                state = LocalMenuState.current,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            BottomSheetPage(
                state = LocalBottomSheetPageState.current,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun EinkNavHost(navController: androidx.navigation.NavHostController) {
    NavHost(
        navController = navController,
        startDestination = EinkScreen.Songs.route,
    ) {
        composable(EinkScreen.Songs.route) { EinkSongsScreen(navController) }
        composable(EinkScreen.Playlists.route) { EinkPlaylistsScreen(navController) }
        composable(EinkScreen.Artists.route) { EinkArtistsScreen(navController) }
        composable(EinkScreen.Albums.route) { EinkAlbumsScreen(navController) }
        composable(EinkScreen.More.route) { EinkMoreScreen(navController) }
        composable(EinkScreen.Settings.route) { EinkSettingsScreen(navController) }
        composable(EinkScreen.Search.route) { EinkSearchScreen(navController) }
        composable(EinkScreen.NowPlaying.route) { EinkNowPlayingScreen(navController) }

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
            EinkPlaylistDetailsScreen(navController, entry.arguments?.getString("playlistId").orEmpty())
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
