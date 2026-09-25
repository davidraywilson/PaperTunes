package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.paperapps.paperui.components.PaperLazyColumn
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.viewmodels.DownloadLibraryViewModel
import moe.rukamori.archivetune.viewmodels.DownloadLibraryScreenState

@Composable
fun EinkDownloadsScreen(
    navController: NavController,
    viewModel: DownloadLibraryViewModel = hiltViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle(null)
    
    val state = screenState as? DownloadLibraryScreenState.Success
    val progressEntries = state?.library?.progressSections?.flatMap { it.entries }.orEmpty()

    if (progressEntries.isEmpty()) {
        EinkEmptyState(
            title = "Downloads",
            body = "No active downloads"
        )
        return
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
        refreshKey = progressEntries,
    ) {
        items(
            count = progressEntries.size,
            key = { index -> progressEntries[index].id }
        ) { index ->
            val entry = progressEntries[index]
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.failed) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = "Failed", tint = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (entry.paused) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.download),
                            contentDescription = "Queued", tint = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        CircularProgressIndicatorMMD(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = entry.title.ifBlank { "Unknown Song" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        val stateText = when {
                            entry.failed -> "Failed"
                            entry.paused -> "Paused / Queued"
                            else -> {
                                val percent = entry.percent
                                if (percent >= 0) "Downloading ($percent%)" else "Downloading..."
                            }
                        }
                        
                        Text(
                            text = stateText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (entry.failed || entry.paused) {
                    IconButton(onClick = { viewModel.resume(entry) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Resume download", tint = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                IconButton(onClick = { viewModel.remove(entry) }) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = "Cancel download", tint = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
