package moe.rukamori.archivetune.eink.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.paperapps.paperui.components.AppbarAction
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.PanoramaHeader
import com.paperapps.paperui.components.PanoramaPager
import com.paperapps.paperui.components.PaperProgressBar
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalSyncUtils
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.eink.EinkOnboardingCompletedKey
import moe.rukamori.archivetune.ui.screens.buildLoginRoute
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.viewmodels.LocalSongsViewModel

// ---------------------------------------------------------------------------
// YouTube sync state for the onboarding YouTube page
// ---------------------------------------------------------------------------
private sealed interface YtSyncState {
    object Idle : YtSyncState
    object Syncing : YtSyncState
    object Done : YtSyncState
    data class Error(val msg: String) : YtSyncState
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EinkOnboardingScreen(
    navController: NavController,
    localSongsViewModel: LocalSongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val titles = listOf("Welcome", "Permissions", "Sync", "YouTube")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val syncUtils = LocalSyncUtils.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var isOnboardingCompleted by rememberPreference(EinkOnboardingCompletedKey, false)

    // Local scan state
    val scanState by localSongsViewModel.scanState.collectAsState()
    // Track whether the user deliberately kicked off a scan on this page
    var localSyncStarted by remember { mutableStateOf(false) }

    // YouTube library sync state
    var ytSyncState by remember { mutableStateOf<YtSyncState>(YtSyncState.Idle) }

    // ---------------------------------------------------------------------------
    // Auto-trigger YouTube library sync when user lands on page 3 while logged in
    // ---------------------------------------------------------------------------
    LaunchedEffect(innerTubeCookie, pagerState.currentPage) {
        if (pagerState.currentPage == 3 && innerTubeCookie.isNotBlank()) {
            if (ytSyncState == YtSyncState.Idle) {
                ytSyncState = YtSyncState.Syncing
                try {
                    syncUtils.performFullSync()
                    ytSyncState = YtSyncState.Done
                    // Brief pause so the user sees the "Done" state, then navigate home
                    delay(1_000)
                    isOnboardingCompleted = true
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ytSyncState = YtSyncState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Permission setup
    // ---------------------------------------------------------------------------
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    // ---------------------------------------------------------------------------
    // Screen layout
    // ---------------------------------------------------------------------------
    Column(modifier = Modifier.fillMaxSize()) {
        PanoramaHeader(
            pagerState = pagerState,
            titles = titles,
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            screenTitle = "Setup"
        )

        PanoramaPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    // ----------------------------------------------------------
                    // Page 0: Welcome
                    // ----------------------------------------------------------
                    0 -> {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = androidx.compose.ui.graphics.Color.Black
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Welcome to PaperTunes",
                            fontSize = 24.sp,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your seamless music experience on E-ink.",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.DarkGray
                        )
                    }

                    // ----------------------------------------------------------
                    // Page 1: Permissions
                    // ----------------------------------------------------------
                    1 -> {
                        Icon(
                            imageVector = Icons.Outlined.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = androidx.compose.ui.graphics.Color.Black
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Local Music Access",
                            fontSize = 24.sp,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isPermissionGranted) "Permission granted!" else "We need permission to access the local music files on your device.",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.DarkGray
                        )
                    }

                    // ----------------------------------------------------------
                    // Page 2: Sync Local Library
                    // ----------------------------------------------------------
                    2 -> {
                        when {
                            scanState.isScanning -> {
                                // Scanning in progress — show real progress bar
                                val progress = if (scanState.totalCount > 0) {
                                    scanState.scannedCount / scanState.totalCount.toFloat()
                                } else {
                                    0f
                                }
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Syncing Local Library",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PaperProgressBar(
                                    progress = progress.coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val countLabel = when {
                                    scanState.totalCount > 0 ->
                                        "${scanState.scannedCount} of ${scanState.totalCount} songs"
                                    else -> "Scanning your device for music files…"
                                }
                                Text(
                                    text = countLabel,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            localSyncStarted && scanState.lastSummary != null -> {
                                // Scan completed successfully
                                val summary = scanState.lastSummary!!
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Sync Complete",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                val songWord = if (summary.scannedSongs == 1) "song" else "songs"
                                Text(
                                    text = "Found ${summary.scannedSongs} $songWord on your device.",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            localSyncStarted && scanState.errorMessage != null -> {
                                // Scan failed
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Sync Local Library",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Scan failed: ${scanState.errorMessage}",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            else -> {
                                // Idle / not yet started
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Sync Local Library",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Would you like to start syncing your local music library now? This will scan your device for songs.",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                        }
                    }

                    // ----------------------------------------------------------
                    // Page 3: YouTube Music
                    // ----------------------------------------------------------
                    3 -> {
                        when {
                            innerTubeCookie.isBlank() -> {
                                // Not logged in — prompt to login
                                Icon(
                                    imageVector = Icons.Outlined.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "YouTube Music",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Log in to YouTube Music to access your online playlists, artists, and library.",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            ytSyncState == YtSyncState.Syncing -> {
                                // Syncing YouTube library
                                CircularProgressIndicatorMMD(
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "YouTube Music",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Syncing your YouTube library…",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            ytSyncState == YtSyncState.Done -> {
                                // Sync done — brief state before auto-nav
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "YouTube Music",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Library synced! Ready to go.",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            ytSyncState is YtSyncState.Error -> {
                                // Sync failed
                                Icon(
                                    imageVector = Icons.Outlined.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "YouTube Music",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Logged in, but sync failed: ${(ytSyncState as YtSyncState.Error).msg}\nYou can still continue.",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                            else -> {
                                // Logged in, idle (shouldn't normally stay here long)
                                Icon(
                                    imageVector = Icons.Outlined.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "YouTube Music",
                                    fontSize = 24.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Logged in to YouTube Music!",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------------
        // Appbar actions — vary per page and sync state
        // ---------------------------------------------------------------------------
        val actions = mutableListOf<AppbarAction>()

        when (pagerState.currentPage) {
            0 -> {
                actions.add(
                    AppbarAction(
                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                        label = "Next",
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                )
            }
            1 -> {
                if (!isPermissionGranted) {
                    actions.add(
                        AppbarAction(
                            icon = Icons.Outlined.Done,
                            label = "Grant",
                            onClick = {
                                permissionLauncher.launch(permissionToRequest)
                            }
                        )
                    )
                }
                actions.add(
                    AppbarAction(
                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                        label = "Next",
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                )
            }
            2 -> {
                if (scanState.isScanning) {
                    // Block all navigation while scanning — no actions shown
                } else {
                    // Show "Sync Now" only if not already started (or on error for retry)
                    val showSyncAction = !localSyncStarted ||
                        scanState.errorMessage != null
                    if (showSyncAction && isPermissionGranted) {
                        actions.add(
                            AppbarAction(
                                icon = Icons.Outlined.Sync,
                                label = if (scanState.errorMessage != null) "Retry" else "Sync Now",
                                onClick = {
                                    localSyncStarted = true
                                    localSongsViewModel.scanDevice()
                                }
                            )
                        )
                    }
                    // Skip / Next — available when not scanning
                    actions.add(
                        AppbarAction(
                            icon = Icons.AutoMirrored.Outlined.ArrowForward,
                            label = if (localSyncStarted && scanState.lastSummary != null) "Next" else "Skip",
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(3)
                                }
                            }
                        )
                    )
                }
            }
            3 -> {
                when {
                    innerTubeCookie.isBlank() -> {
                        // Not logged in
                        actions.add(
                            AppbarAction(
                                icon = Icons.Outlined.Check,
                                label = "Login",
                                onClick = {
                                    navController.navigate(buildLoginRoute())
                                }
                            )
                        )
                        actions.add(
                            AppbarAction(
                                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                label = "Finish",
                                onClick = {
                                    isOnboardingCompleted = true
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        )
                    }
                    ytSyncState == YtSyncState.Syncing -> {
                        // No actions while syncing
                    }
                    ytSyncState == YtSyncState.Done -> {
                        // Finish action immediately available (auto-nav handles it too)
                        actions.add(
                            AppbarAction(
                                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                label = "Finish",
                                onClick = {
                                    isOnboardingCompleted = true
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        )
                    }
                    ytSyncState is YtSyncState.Error -> {
                        // Retry + Finish (let user proceed despite error)
                        actions.add(
                            AppbarAction(
                                icon = Icons.Outlined.Refresh,
                                label = "Retry",
                                onClick = {
                                    ytSyncState = YtSyncState.Idle
                                    // The LaunchedEffect will re-trigger on next recomposition
                                    // because ytSyncState changed back to Idle.
                                    // We force a recompose by toggling through coroutine scope.
                                    coroutineScope.launch {
                                        ytSyncState = YtSyncState.Syncing
                                        try {
                                            syncUtils.performFullSync()
                                            ytSyncState = YtSyncState.Done
                                            delay(1_000)
                                            isOnboardingCompleted = true
                                            navController.navigate("home") {
                                                popUpTo("onboarding") { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            ytSyncState = YtSyncState.Error(e.message ?: "Unknown error")
                                        }
                                    }
                                }
                            )
                        )
                        actions.add(
                            AppbarAction(
                                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                label = "Finish",
                                onClick = {
                                    isOnboardingCompleted = true
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        )
                    }
                    else -> {
                        // Logged in + Idle (transitional) — no actions while LaunchedEffect fires
                    }
                }
            }
        }

        ApplicationBar(
            actions = actions,
            pagerState = pagerState
        )
    }
}
