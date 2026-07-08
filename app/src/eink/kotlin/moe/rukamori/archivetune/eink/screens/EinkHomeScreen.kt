package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.paperapps.paperui.components.PanoramaPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.paperapps.paperui.components.AppbarAction
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.PanoramaHeader
import moe.rukamori.archivetune.eink.EinkScreen
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.eink.components.EinkNowPlayingButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EinkHomeScreen(
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val titles = listOf("Playlists", "Songs", "Artists", "Albums")
    val pagerState = rememberPagerState(pageCount = { titles.size })

    // Playlists edit mode state
    var isPlaylistsEditMode by remember { mutableStateOf(false) }
    var playlistEditSelectionCount by remember { mutableIntStateOf(0) }
    val playlistEditSelectionIds = remember { mutableSetOf<String>() }
    var showDeletePlaylistsConfirmation by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        PanoramaHeader(
            pagerState = pagerState,
            titles = titles,
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth()
        )

        PanoramaPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> EinkPlaylistsScreen(
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
                    selectedIds = playlistEditSelectionIds,
                    showSortSheet = showSortSheet,
                    onDismissSortSheet = { showSortSheet = false }
                )
                1 -> EinkSongsScreen(navController)
                2 -> EinkArtistsScreen(navController)
                3 -> EinkAlbumsScreen(navController)
            }
        }

        val baseActions = listOf(
            AppbarAction(
                icon = Icons.Outlined.Search,
                label = "Search",
                onClick = { navController.navigate(EinkScreen.Search.route) }
            ),
            AppbarAction(
                icon = Icons.Outlined.Download,
                label = "Downloads",
                onClick = { navController.navigate(EinkScreen.Downloads.route) }
            ),
            AppbarAction(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = { navController.navigate(EinkScreen.Settings.route) }
            )
        )

        ApplicationBar(
            actions = if (pagerState.currentPage == 0) {
                listOf(
                    AppbarAction(
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        label = "Sort",
                        onClick = { showSortSheet = true }
                    )
                ) + baseActions
            } else {
                baseActions
            },
            menuItems = emptyList(), // Can add playlist edit actions here based on current page
            leftSlot = { EinkNowPlayingButton(navController) }
        )
    }
}
