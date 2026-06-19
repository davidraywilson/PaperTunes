package moe.rukamori.archivetune.eink.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.offline.Download
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.eink.AutoDownloadPlaylistsKey
import moe.rukamori.archivetune.ui.utils.HeaderDownloadItem
import moe.rukamori.archivetune.ui.utils.sendAddMissingDownloads
import moe.rukamori.archivetune.utils.dataStore
import timber.log.Timber

@Composable
fun EinkAutoDownloadObserver() {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current

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
                                val missingSongs = songs.filter { item ->
                                    val state = downloads[item.song.id]?.state
                                    state != Download.STATE_COMPLETED &&
                                            state != Download.STATE_DOWNLOADING &&
                                            state != Download.STATE_QUEUED
                                }

                                if (missingSongs.isNotEmpty()) {
                                    Timber.d("EinkAutoDownloadObserver: Triggering download for %d missing songs in %s", missingSongs.size, playlistId)
                                    sendAddMissingDownloads(
                                        context = context,
                                        songs = missingSongs.map {
                                            HeaderDownloadItem(
                                                id = it.song.id,
                                                title = it.song.title
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
