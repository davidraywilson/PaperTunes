/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.papertunes.eink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import moe.rukamori.papertunes.eink.screens.EinkOnboardingScreen
import moe.rukamori.papertunes.utils.rememberPreference
import moe.rukamori.papertunes.eink.components.EinkAutoDownloadObserver
import moe.rukamori.papertunes.eink.screens.EinkAlbumDetailsScreen
import moe.rukamori.papertunes.eink.screens.EinkArtistDetailsScreen
import moe.rukamori.papertunes.eink.screens.EinkHomeScreen
import moe.rukamori.papertunes.eink.screens.EinkMoreScreen
import moe.rukamori.papertunes.eink.screens.EinkNowPlayingScreen
import moe.rukamori.papertunes.eink.screens.EinkPlaylistAddSongsScreen
import moe.rukamori.papertunes.eink.screens.EinkPlaylistDetailsScreen
import moe.rukamori.papertunes.eink.screens.EinkPlaylistEditScreen
import moe.rukamori.papertunes.eink.screens.EinkSearchScreen
import moe.rukamori.papertunes.eink.screens.EinkSettingsScreen
import moe.rukamori.papertunes.eink.screens.EinkYouTubeArtistScreen
import moe.rukamori.papertunes.ui.screens.LOGIN_ROUTE
import moe.rukamori.papertunes.ui.screens.LOGIN_URL_ARGUMENT
import moe.rukamori.papertunes.ui.screens.LoginScreen

/** Route helpers for destinations that take an id argument. */
private fun detailRoute(base: String, argName: String) = "$base/{$argName}"

fun einkAlbumDetailsRoute(albumId: String) = "${EinkScreen.AlbumDetails.route}/$albumId"
fun einkArtistDetailsRoute(artistId: String) = "${EinkScreen.ArtistDetails.route}/$artistId"
fun einkPlaylistDetailsRoute(playlistId: String) = "${EinkScreen.PlaylistDetails.route}/$playlistId"
fun einkPlaylistEditRoute(playlistId: String) = "${EinkScreen.PlaylistEdit.route}/$playlistId"
fun einkPlaylistAddSongsRoute(playlistId: String) = "${EinkScreen.PlaylistAddSongs.route}/$playlistId"
fun einkYouTubeArtistDetailsRoute(artistId: String) = "${EinkScreen.YouTubeArtistDetails.route}/$artistId"

@Composable
fun EinkApp() {
    val navController = rememberNavController()
    val isOnboardingCompleted by rememberPreference(EinkOnboardingCompletedKey, false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        EinkAutoDownloadObserver()
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.White, contentColor = androidx.compose.ui.graphics.Color.Black) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.White)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = if (isOnboardingCompleted) "home" else "onboarding",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable("onboarding") { EinkOnboardingScreen(navController) }
                    composable("home") { EinkHomeScreen(navController) }
                    composable(EinkScreen.More.route) { EinkMoreScreen(navController) }
                    composable(EinkScreen.Settings.route) { EinkSettingsScreen(navController) }
                    composable(EinkScreen.Search.route) { EinkSearchScreen(navController, androidx.lifecycle.viewmodel.compose.viewModel()) }
                    composable(EinkScreen.NowPlaying.route) { EinkNowPlayingScreen(navController) }
                    composable(EinkScreen.Downloads.route) { moe.rukamori.papertunes.eink.screens.EinkDownloadsScreen(navController) }

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
                        route = detailRoute(EinkScreen.YouTubeArtistDetails.route, "artistId"),
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
                    ) { entry ->
                        EinkYouTubeArtistScreen(navController, entry.arguments?.getString("artistId").orEmpty())
                    }
                    composable(
                        route = detailRoute(EinkScreen.PlaylistDetails.route, "playlistId"),
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                    ) { entry ->
                        EinkPlaylistDetailsScreen(
                            navController = navController, 
                            playlistId = entry.arguments?.getString("playlistId").orEmpty()
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
    }
}
