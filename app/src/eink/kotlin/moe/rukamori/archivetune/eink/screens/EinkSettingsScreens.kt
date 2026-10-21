/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavController
import com.paperapps.paperui.components.PaperLazyColumn
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.App.Companion.forgetAccount
import moe.rukamori.archivetune.LocalSyncUtils
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.eink.EinkScreen
import moe.rukamori.archivetune.innertube.utils.hasYouTubeLoginCookie
import moe.rukamori.archivetune.ui.screens.buildLoginRoute
import moe.rukamori.archivetune.utils.rememberPreference

import moe.rukamori.archivetune.canvas.CanvasSource
import moe.rukamori.archivetune.constants.CanvasSourceKey
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow
import moe.rukamori.archivetune.eink.components.EinkToggleRow
import moe.rukamori.archivetune.spotify.SpotifyAccountViewModel
import moe.rukamori.archivetune.viewmodels.LocalSongsViewModel

/*
 * Owned by the `settings` child agent. Replace these stub bodies with the real e-ink
 * More + Settings screens. Keep the composable names and signatures identical (EinkApp's
 * NavHost references them). You may add more files in this package; just ensure each
 * composable below is defined exactly once across the module.
 */

@Composable
fun EinkMoreScreen(navController: NavController) {
    PaperLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
    ) {
        item {
            EinkTwoLineRow(
                title = "Downloads",
                subtitle = "Manage queued and active downloads",
                onClick = { navController.navigate(EinkScreen.Downloads.route) },
                showDivider = true
            )
            EinkTwoLineRow(
                title = "Settings",
                subtitle = "App preferences and options",
                onClick = { navController.navigate(EinkScreen.Settings.route) },
                showDivider = false
            )
        }
    }
}

@Composable
fun EinkSettingsScreen(
    navController: NavController,
    localViewModel: LocalSongsViewModel = hiltViewModel(),
    spotifyAccountViewModel: SpotifyAccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = hasYouTubeLoginCookie(innerTubeCookie)
    val spotifyState by spotifyAccountViewModel.uiState.collectAsState()
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    
    // val (forceHighQuality, onForceHighQualityChange) = rememberPreference(ForceHighQualityDownloadsKey, false)

    val scanState by localViewModel.scanState.collectAsState()
    
    val storagePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasStoragePermission by remember(storagePermission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            localViewModel.scanDevice()
        }
    }

    PaperLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
    ) {
        item(key = "login") {
            EinkTwoLineRow(
                title = "YouTube Login",
                subtitle = if (isLoggedIn) "Logged in (Tap to logout)" else "Not logged in (Tap to login)",
                onClick = {
                    if (isLoggedIn) {
                        onInnerTubeCookieChange("")
                        forgetAccount(context, clearWebAuthSession = true)
                    } else {
                        navController.navigate(buildLoginRoute())
                    }
                },
                showDivider = true
            )
        }

        item(key = "spotify_login") {
            val spotifySubtitle = when {
                spotifyState.isLoading -> "Connecting..."
                spotifyState.isAuthenticated -> "Logged in as ${spotifyState.accountName.ifBlank { "Spotify User" }} (Tap to logout)"
                else -> "Not logged in (Tap to login for Spotify Canvas)"
            }
            EinkTwoLineRow(
                title = "Spotify Account",
                subtitle = spotifySubtitle,
                onClick = {
                    if (spotifyState.isAuthenticated) {
                        spotifyAccountViewModel.logout()
                        android.widget.Toast.makeText(context, "Logged out of Spotify", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        navController.navigate(EinkScreen.SpotifyLogin.route)
                    }
                },
                showDivider = true
            )
        }

        item {
            val scanSubtitle = when {
                scanState.isScanning -> "Scanning device for audio files..."
                scanState.errorMessageRes != null -> "Scan failed: ${androidx.compose.ui.res.stringResource(scanState.errorMessageRes!!)}"
                scanState.lastSummary != null -> {
                    val summary = scanState.lastSummary!!
                    "Last scan: Found ${summary.scannedSongs} new songs."
                }
                !hasStoragePermission -> "Tap to grant permission and scan"
                else -> "Scan device for local audio files"
            }
            EinkTwoLineRow(
                title = "Local Audio Files",
                subtitle = scanSubtitle,
                onClick = {
                    if (scanState.isScanning) return@EinkTwoLineRow
                    if (!hasStoragePermission) {
                        permissionLauncher.launch(storagePermission)
                    } else {
                        localViewModel.scanDevice()
                    }
                },
                showDivider = true
            )
        }

        item(key = "canvas") {
            val (canvasEnabled, onCanvasEnabledChange) = moe.rukamori.archivetune.utils.rememberPreference(moe.rukamori.archivetune.constants.ArchiveTuneCanvasKey, true)
            val canvasSubtitle = if (spotifyState.isAuthenticated) {
                "Display canvas looping videos (Spotify priority active)"
            } else {
                "Display short looping videos (BetterLyrics / Apple Music)"
            }
            EinkToggleRow(
                title = "Canvas Artwork",
                subtitle = canvasSubtitle,
                checked = canvasEnabled,
                onCheckedChange = { onCanvasEnabledChange(it) },
                showDivider = true
            )
        }

        item(key = "canvasSource") {
            val (canvasSourceStr, onCanvasSourceChange) = moe.rukamori.archivetune.utils.rememberPreference(
                CanvasSourceKey,
                "ALL"
            )
            val currentSource = CanvasSource.fromPreference(canvasSourceStr)
            val sourceSubtitle = when (currentSource) {
                CanvasSource.ALL -> if (spotifyState.isAuthenticated) "All (Spotify first, fallback to others)" else "All (BetterLyrics, Apple Music, Tidal)"
                CanvasSource.SPOTIFY -> if (spotifyState.isAuthenticated) "Spotify only" else "Spotify only (requires login)"
                CanvasSource.BETTER_LYRICS -> "BetterLyrics only"
                CanvasSource.APPLE_MUSIC -> "Apple Music only"
                CanvasSource.TIDAL -> "Tidal only"
            }
            EinkTwoLineRow(
                title = "Canvas Source",
                subtitle = sourceSubtitle,
                onClick = {
                    val nextSource = when (currentSource) {
                        CanvasSource.ALL -> CanvasSource.SPOTIFY
                        CanvasSource.SPOTIFY -> CanvasSource.BETTER_LYRICS
                        CanvasSource.BETTER_LYRICS -> CanvasSource.APPLE_MUSIC
                        CanvasSource.APPLE_MUSIC -> CanvasSource.TIDAL
                        CanvasSource.TIDAL -> CanvasSource.ALL
                    }
                    onCanvasSourceChange(nextSource.name)
                },
                showDivider = true
            )
        }

        item(key = "canvasWifi") {
            val (wifiOnly, onWifiOnlyChange) = moe.rukamori.archivetune.utils.rememberPreference(moe.rukamori.archivetune.constants.CanvasWifiOnlyKey, false)
            EinkToggleRow(
                title = "Canvas WiFi Only",
                subtitle = "Only download canvas on Wi-Fi",
                checked = wifiOnly,
                onCheckedChange = { onWifiOnlyChange(it) },
                showDivider = isLoggedIn
            )
        }

        if (isLoggedIn) {
            item {
                EinkTwoLineRow(
                    title = "Sync Library",
                    subtitle = if (isSyncing) "Syncing..." else "Fetch playlists and songs from YouTube",
                    onClick = {
                        if (!isSyncing) {
                            coroutineScope.launch {
                                isSyncing = true
                                android.widget.Toast.makeText(context, "Sync started", android.widget.Toast.LENGTH_SHORT).show()
                                try {
                                    syncUtils.performFullSync()
                                    android.widget.Toast.makeText(context, "Sync complete", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    android.widget.Toast.makeText(context, "Sync failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSyncing = false
                                }
                            }
                        }
                    },
                    showDivider = true
                )
            }
        }
        

    }
}
