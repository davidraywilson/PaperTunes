package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.playback.ExoDownloadService

@Composable
fun EinkDownloadsScreen(navController: NavController) {
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current
    val downloadsMap by downloadUtil.downloads.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    
    val hasActiveDownloads = remember(downloadsMap) {
        downloadsMap.values.any { it.state == Download.STATE_DOWNLOADING }
    }
    
    LaunchedEffect(hasActiveDownloads) {
        if (hasActiveDownloads) {
            while (true) {
                delay(3000)
                tick++
            }
        }
    }
    
    val queuedDownloads = remember(downloadsMap, tick) {
        val freshActiveDownloads = downloadUtil.downloadManager.currentDownloads.associateBy { it.request.id }
        
        downloadsMap.values.map { download ->
            freshActiveDownloads[download.request.id] ?: download
        }.filter { 
            it.state == Download.STATE_QUEUED || 
            it.state == Download.STATE_DOWNLOADING ||
            it.state == Download.STATE_FAILED ||
            it.state == Download.STATE_RESTARTING ||
            it.state == Download.STATE_STOPPED
        }.sortedByDescending { it.updateTimeMs }
    }

    if (queuedDownloads.isEmpty()) {
        EinkEmptyState(
            title = "Downloads",
            body = "No active downloads"
        )
        return
    }

    LazyColumnMMD(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = queuedDownloads.size,
            key = { index -> queuedDownloads[index].request.id }
        ) { index ->
            val download = queuedDownloads[index]
            val title = String(download.request.data)
            
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
                    if (download.state == Download.STATE_DOWNLOADING) {
                        CircularProgressIndicatorMMD(
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (download.state == Download.STATE_FAILED) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = "Failed",
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(moe.rukamori.archivetune.R.drawable.download),
                            contentDescription = "Queued",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = title.ifBlank { "Unknown Song" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        val stateText = when (download.state) {
                            Download.STATE_QUEUED -> "Queued"
                            Download.STATE_DOWNLOADING -> {
                                val percent = download.percentDownloaded
                                if (percent >= 0f) "Downloading (${percent.toInt()}%)" else "Downloading..."
                            }
                            Download.STATE_FAILED -> "Failed"
                            Download.STATE_STOPPED -> "Paused"
                            Download.STATE_RESTARTING -> "Restarting"
                            else -> "Processing"
                        }
                        
                        Text(
                            text = stateText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = {
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        download.request.id,
                        false
                    )
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = "Cancel download",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
