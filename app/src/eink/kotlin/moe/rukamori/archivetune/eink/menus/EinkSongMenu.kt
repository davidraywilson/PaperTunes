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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import moe.rukamori.archivetune.eink.einkYouTubeArtistDetailsRoute
import moe.rukamori.archivetune.eink.einkAlbumDetailsRoute
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalDownloadUtil
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
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val isLocalSong = originalSong.song.isLocal
    val downloadsMap by downloadUtil.downloads.collectAsState()
    val downloadState = downloadsMap[originalSong.id]?.state

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    if (showAddToPlaylistDialog) {
        moe.rukamori.archivetune.eink.components.EinkAddToPlaylistDialog(
            mediaMetadata = originalSong.toMediaMetadata(),
            onDismiss = {
                showAddToPlaylistDialog = false
                onDismiss()
            }
        )
    }

    DropdownMenuItemMMD(
        text = { TextMMD(text = "Add to Playlist", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            showAddToPlaylistDialog = true
        }
    )
    
    DashedDivider(thickness = 1.dp)

    DropdownMenuItemMMD(
        text = { TextMMD(text = "Play Next", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onDismiss()
            playerConnection.playNext(originalSong.toMediaItem())
        }
    )
    
    DashedDivider(thickness = 1.dp)

    DropdownMenuItemMMD(
        text = { TextMMD(text = "Add to Queue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onDismiss()
            playerConnection.addToQueue(originalSong.toMediaItem())
        }
    )

    if (!isLocalSong) {
        DashedDivider(thickness = 1.dp)
        DropdownMenuItemMMD(
            text = { TextMMD(text = "Start Radio", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                playerConnection.playQueue(YouTubeQueue.radio(originalSong.toMediaMetadata()))
            }
        )
    }

    DashedDivider(thickness = 1.dp)

    if (downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING || downloadState == Download.STATE_QUEUED) {
        DropdownMenuItemMMD(
            text = { TextMMD(text = "Remove Download", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                DownloadService.sendRemoveDownload(
                    context,
                    moe.rukamori.archivetune.playback.ExoDownloadService::class.java,
                    originalSong.id,
                    false
                )
            }
        )
    } else {
        DropdownMenuItemMMD(
            text = { TextMMD(text = "Download", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                database.transaction {
                    insert(originalSong.toMediaMetadata())
                }
                val req = DownloadRequest
                    .Builder(originalSong.id, originalSong.id.toUri())
                    .setCustomCacheKey(originalSong.id)
                    .setData(originalSong.song.title.toByteArray())
                    .build()
                DownloadService.sendAddDownload(
                    context,
                    moe.rukamori.archivetune.playback.ExoDownloadService::class.java,
                    req,
                    false
                )
            }
        )
    }

    val artist = originalSong.artists.firstOrNull()
    if (artist != null) {
        DashedDivider(thickness = 1.dp)
        DropdownMenuItemMMD(
            text = { TextMMD(text = "View Artist", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                navController.navigate(einkYouTubeArtistDetailsRoute(artist.id))
            }
        )
    }

    if (originalSong.song.albumId != null) {
        DashedDivider(thickness = 1.dp)
        DropdownMenuItemMMD(
            text = { TextMMD(text = "View Album", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                navController.navigate(einkAlbumDetailsRoute(originalSong.song.albumId))
            }
        )
    }
}


