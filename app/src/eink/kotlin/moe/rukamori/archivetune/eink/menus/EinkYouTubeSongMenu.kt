/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.menus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.eink.components.DashedDivider
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.playback.queues.YouTubeQueue

import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.playback.ExoDownloadService

@Composable
fun EinkYouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current

    Column(modifier = Modifier.fillMaxWidth()) {
        TextMMD(
            text = song.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextMMD(
            text = song.artists.joinToString { it.name },
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        DashedDivider(thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        EinkMenuRow(text = "Play Next") {
            onDismiss()
            playerConnection.playNext(song.toMediaItem())
        }
        
        EinkMenuRow(text = "Add to Queue") {
            onDismiss()
            playerConnection.addToQueue(song.toMediaItem())
        }

        EinkMenuRow(text = "Start Radio") {
            onDismiss()
            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
        }

        val artist = song.artists.firstOrNull()
        if (artist?.id != null) {
            EinkMenuRow(text = "View Artist") {
                onDismiss()
                navController.navigate("artist/${artist.id}")
            }
        }

        val album = song.album
        if (album?.id != null) {
            EinkMenuRow(text = "View Album") {
                onDismiss()
                navController.navigate("album/${album.id}")
            }
        }

        EinkMenuRow(text = "Download") {
            onDismiss()
            database.transaction {
                insert(song.toMediaMetadata())
            }
            val downloadRequest = DownloadRequest
                .Builder(song.id, song.id.toUri())
                .setCustomCacheKey(song.id)
                .setData(song.title.toByteArray())
                .build()
            DownloadService.sendAddDownload(
                context,
                ExoDownloadService::class.java,
                downloadRequest,
                false,
            )
        }
    }
}

@Composable
private fun EinkMenuRow(
    text: String,
    onClick: () -> Unit,
) {
    TextMMD(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    )
}
