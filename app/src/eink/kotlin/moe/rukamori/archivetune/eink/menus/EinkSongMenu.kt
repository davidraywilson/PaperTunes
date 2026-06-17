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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.db.entities.PlaylistSong
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.eink.components.DashedDivider
import moe.rukamori.archivetune.eink.components.songSubtitle
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.playback.queues.YouTubeQueue

@Composable
fun EinkSongMenu(
    originalSong: Song,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isLocalSong = originalSong.song.isLocal

    // TODO: Add EinkAddToPlaylistDialog support if needed later.
    // For now, providing the main playback actions to decouple the UI.

    Column(modifier = Modifier.fillMaxWidth()) {
        TextMMD(
            text = originalSong.song.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextMMD(
            text = songSubtitle(originalSong),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        DashedDivider(thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        EinkMenuRow(text = "Play Next") {
            onDismiss()
            playerConnection.playNext(originalSong.toMediaItem())
        }
        
        EinkMenuRow(text = "Add to Queue") {
            onDismiss()
            playerConnection.addToQueue(originalSong.toMediaItem())
        }

        if (!isLocalSong) {
            EinkMenuRow(text = "Start Radio") {
                onDismiss()
                playerConnection.playQueue(YouTubeQueue.radio(originalSong.toMediaMetadata()))
            }
        }

        val artist = originalSong.artists.firstOrNull()
        if (artist != null) {
            EinkMenuRow(text = "View Artist") {
                onDismiss()
                navController.navigate("artist/${artist.id}")
            }
        }

        if (originalSong.song.albumId != null) {
            EinkMenuRow(text = "View Album") {
                onDismiss()
                navController.navigate("album/${originalSong.song.albumId}")
            }
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
