package moe.rukamori.papertunes.eink.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.offline.Download
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.rukamori.papertunes.LocalDatabase
import moe.rukamori.papertunes.LocalDownloadUtil
import moe.rukamori.papertunes.eink.AutoDownloadPlaylistsKey
import moe.rukamori.papertunes.utils.dataStore
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
                            database.playlistSongs(playlistId)
                                .distinctUntilChanged { old, new -> old.map { it.song.id } == new.map { it.song.id } }
                                .collectLatest { songs ->
                                    if (songs.isEmpty()) return@collectLatest

                                    // Wait for ExoPlayer's DownloadManager to finish its initial
                                    // database load. Using .value or plain .first() returns the
                                    // stale emptyMap() that the StateFlow holds at app startup,
                                    // causing every song to appear un-downloaded and get re-queued.
                                    // first { it.isNotEmpty() } suspends until the manager has
                                    // reported real state; in the steady state the StateFlow already
                                    // holds a non-empty map so this returns immediately at zero cost.
                                    val downloads = downloadUtil.downloads
                                        .first { it.isNotEmpty() }

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
                                        
                                        moe.rukamori.papertunes.ui.utils.sendAddMissingDownloads(
                                            context = context,
                                            songs = missingSongs.map {
                                                moe.rukamori.papertunes.ui.utils.HeaderDownloadItem(
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
