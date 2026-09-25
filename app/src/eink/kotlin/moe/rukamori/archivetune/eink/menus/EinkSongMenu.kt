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
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import java.time.LocalDateTime
import moe.rukamori.archivetune.R
import com.paperapps.paperui.components.DashedDivider
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
    val coroutineScope = rememberCoroutineScope()
    val librarySong by database.song(originalSong.id).collectAsState(initial = null)
    val isLibrarySong = librarySong?.song?.inLibrary != null
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

    if (!isLocalSong) {
        
        DropdownMenuItemMMD(
            text = { TextMMD(text = if (isLibrarySong) "Remove from Library" else "Add to Library", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            onClick = {
                onDismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    val shouldAdd = !isLibrarySong
                    val remoteResult = YouTube.likeVideo(originalSong.id, shouldAdd)
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
                        val base = librarySong?.song ?: originalSong.song
                        if (librarySong == null) {
                            insert(
                                originalSong.toMediaMetadata(),
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
                
                if (!isLocalSong && !isLibrarySong) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val remoteResult = YouTube.likeVideo(originalSong.id, true)
                        if (remoteResult.isSuccess) {
                            val now = LocalDateTime.now()
                            database.withTransaction {
                                val base = librarySong?.song ?: originalSong.song
                                if (librarySong == null) {
                                    insert(originalSong.toMediaMetadata(), { it.toggleLibrary() })
                                } else {
                                    update(base.copy(inLibrary = now))
                                }
                                // CUSTOM: E-Ink Library — also bookmark the song's album so it
                                // appears immediately in the eink Albums tab (albumsLiked filter).
                                val albumId = originalSong.song.albumId
                                if (albumId != null) {
                                    val existingAlbum = getAlbumEntitiesByIds(listOf(albumId)).firstOrNull()
                                    if (existingAlbum != null && existingAlbum.bookmarkedAt == null) {
                                        update(existingAlbum.copy(bookmarkedAt = now))
                                    }
                                }
                            }
                        }
                    }
                }

                moe.rukamori.archivetune.ui.utils.sendAddMissingDownloads(context, listOf(moe.rukamori.archivetune.ui.utils.HeaderDownloadItem(originalSong.id, originalSong.song.title)), downloadsMap)
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


