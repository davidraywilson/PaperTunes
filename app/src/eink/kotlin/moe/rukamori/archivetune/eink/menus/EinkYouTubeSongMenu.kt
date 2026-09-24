/*
 * PaperTunes (2026)
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
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalPlayerConnection
import com.paperapps.paperui.components.DashedDivider
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.playback.queues.YouTubeQueue
import moe.rukamori.archivetune.eink.einkYouTubeArtistDetailsRoute
import moe.rukamori.archivetune.eink.EinkScreen

import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import moe.rukamori.archivetune.playback.ExoDownloadService
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.db.entities.SongEntity
import moe.rukamori.archivetune.innertube.YouTube
import java.time.LocalDateTime
import moe.rukamori.archivetune.R

@Composable
fun EinkYouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val librarySong by database.song(song.id).collectAsState(initial = null)

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    if (showAddToPlaylistDialog) {
        moe.rukamori.archivetune.eink.components.EinkAddToPlaylistDialog(
            mediaMetadata = song.toMediaMetadata(),
            onDismiss = {
                showAddToPlaylistDialog = false
                onDismiss()
            }
        )
    }

    // Dropdown items
    val isLibrarySong = librarySong?.song?.inLibrary != null
    DropdownMenuItemMMD(
        text = { TextMMD(text = if (isLibrarySong) "Remove from Library" else "Add to Library", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onDismiss()
            CoroutineScope(Dispatchers.IO).launch {
                val shouldAdd = !isLibrarySong
                val remoteResult = YouTube.likeVideo(song.id, shouldAdd)
                if (remoteResult.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(context, context.getString(R.string.error_unknown), Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@launch
                }

                val now = LocalDateTime.now()
                database.withTransaction {
                    val base = librarySong?.song ?: song.toMediaMetadata().toSongEntity()
                    if (librarySong == null) {
                        insert(
                            song.toMediaMetadata(),
                            if (shouldAdd) {
                                { it.toggleLibrary() }
                            } else {
                                { it }
                            }
                        )
                    } else {
                        update(
                            base.copy(
                                inLibrary = if (shouldAdd) now else null
                            )
                        )
                    }
                }
            }
        }
    )
    
    DashedDivider(thickness = 1.dp)
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
            playerConnection.playNext(song.toMediaItem())
        }
    )
    
    DashedDivider(thickness = 1.dp)

    DropdownMenuItemMMD(
        text = { TextMMD(text = "Add to Queue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onDismiss()
            playerConnection.addToQueue(song.toMediaItem())
        }
    )

    DashedDivider(thickness = 1.dp)

    DropdownMenuItemMMD(
        text = { TextMMD(text = "Start Radio", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onDismiss()
            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
        }
    )

    val artist = song.artists.firstOrNull()
    val artistId = artist?.id
    if (artistId != null) {
        DashedDivider(thickness = 1.dp)
        DropdownMenuItemMMD(
            text = { TextMMD(text = "View Artist", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                navController.navigate(einkYouTubeArtistDetailsRoute(artistId))
            }
        )
    }

    val album = song.album
    if (album?.id != null) {
        DashedDivider(thickness = 1.dp)
        DropdownMenuItemMMD(
            text = { TextMMD(text = "View Album", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                navController.navigate("${EinkScreen.AlbumDetails.route}/${album.id}")
            }
        )
    }

    DashedDivider(thickness = 1.dp)

    DropdownMenuItemMMD(
        text = { TextMMD(text = "Download", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onDismiss()
            
            if (!isLibrarySong) {
                CoroutineScope(Dispatchers.IO).launch {
                    val remoteResult = YouTube.likeVideo(song.id, true)
                    if (remoteResult.isSuccess) {
                        val now = LocalDateTime.now()
                        database.withTransaction {
                            val base = librarySong?.song ?: song.toMediaMetadata().toSongEntity()
                            if (librarySong == null) {
                                insert(song.toMediaMetadata(), { it.toggleLibrary() })
                            } else {
                                update(base.copy(inLibrary = now))
                            }
                        }
                    }
                }
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
    )
}

