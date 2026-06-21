package moe.rukamori.archivetune.eink.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.eink.AutoDownloadPlaylistsKey
import moe.rukamori.archivetune.playback.ExoDownloadService
import moe.rukamori.archivetune.utils.dataStore
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Composable
fun EinkAutoDownloadObserver() {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current

    // Track recently requested downloads to prevent an infinite loop where the database
    // flow emits before ExoPlayer updates its download state.
    val requestedDownloads = remember { Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>()) }

    LaunchedEffect(Unit) {
        context.dataStore.data
            .map { it[AutoDownloadPlaylistsKey] ?: emptySet() }
            .distinctUntilChanged()
            .collectLatest { autoDownloadIds ->
                if (autoDownloadIds.isEmpty()) return@collectLatest

                coroutineScope {
                    for (playlistId in autoDownloadIds) {
                        launch {
                            database.playlistSongs(playlistId).collectLatest { songs ->
                                if (songs.isEmpty()) return@collectLatest

                                val downloads = downloadUtil.downloads.value
                                val missingSongs = songs.filter { song ->
                                    !song.song.song.isLocal &&
                                    !requestedDownloads.contains(song.song.id) &&
                                    when (downloads[song.song.id]?.state) {
                                        Download.STATE_COMPLETED,
                                        Download.STATE_QUEUED,
                                        Download.STATE_DOWNLOADING,
                                        Download.STATE_RESTARTING -> false
                                        else -> true
                                    }
                                }

                                if (missingSongs.isNotEmpty()) {
                                    Timber.d("EinkAutoDownloadObserver: Triggering download for %d missing songs in %s", missingSongs.size, playlistId)
                                    
                                    missingSongs.forEach { requestedDownloads.add(it.song.id) }
                                    
                                    moe.rukamori.archivetune.ui.utils.sendAddMissingDownloads(
                                        context = context,
                                        songs = missingSongs.map {
                                            moe.rukamori.archivetune.ui.utils.HeaderDownloadItem(
                                                id = it.song.id,
                                                title = it.song.song.title
                                            )
                                        },
                                        downloads = downloads
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}
