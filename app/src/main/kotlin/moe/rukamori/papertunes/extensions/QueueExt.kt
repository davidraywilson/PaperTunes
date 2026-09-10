/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.papertunes.extensions

import moe.rukamori.papertunes.innertube.models.WatchEndpoint
import moe.rukamori.papertunes.models.MediaMetadata
import moe.rukamori.papertunes.models.PersistQueue
import moe.rukamori.papertunes.models.QueueData
import moe.rukamori.papertunes.models.QueueType
import moe.rukamori.papertunes.playback.queues.ListQueue
import moe.rukamori.papertunes.playback.queues.LocalAlbumRadio
import moe.rukamori.papertunes.playback.queues.Queue
import moe.rukamori.papertunes.playback.queues.YouTubeAlbumRadio
import moe.rukamori.papertunes.playback.queues.YouTubeQueue

fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long,
): PersistQueue =
    when (this) {
        is ListQueue -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LIST,
            )
        }

        is YouTubeQueue -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE,
                queueData =
                    QueueData.YouTubeData(
                        videoId = endpoint.videoId,
                        playlistId = endpoint.playlistId,
                        endpointParams = endpoint.params,
                        followAutomixPreview = followAutomixPreview,
                    ),
            )
        }

        is YouTubeAlbumRadio -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE_ALBUM_RADIO,
                queueData =
                    QueueData.YouTubeAlbumRadioData(
                        playlistId = playlistId,
                        albumSongCount = albumSongCount,
                        continuation = continuation,
                        firstTimeLoaded = firstTimeLoaded,
                    ),
            )
        }

        is LocalAlbumRadio -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LOCAL_ALBUM_RADIO,
                queueData =
                    QueueData.LocalAlbumRadioData(
                        albumId = albumWithSongs.album.id,
                        startIndex = startIndex,
                    ),
            )
        }

        else -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LIST,
            )
        }
    }

fun PersistQueue.toQueue(): Queue =
    ListQueue(
        title = title,
        items = items.map { it.toMediaItem() },
        startIndex = mediaItemIndex,
        position = position,
    )

fun PersistQueue.toContinuationQueue(): Queue =
    when (queueType) {
        is QueueType.LIST -> {
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position,
            )
        }

        is QueueType.YOUTUBE -> {
            val data =
                queueData as? QueueData.YouTubeData
                    ?: return ListQueue(title, items.map { it.toMediaItem() }, mediaItemIndex, position)
            YouTubeQueue(
                endpoint =
                    WatchEndpoint(
                        videoId = data.videoId,
                        playlistId = data.playlistId,
                        params = data.endpointParams,
                    ),
                followAutomixPreview = data.followAutomixPreview,
            )
        }

        is QueueType.YOUTUBE_ALBUM_RADIO -> {
            val data =
                queueData as? QueueData.YouTubeAlbumRadioData
                    ?: return ListQueue(title, items.map { it.toMediaItem() }, mediaItemIndex, position)
            YouTubeAlbumRadio(
                playlistId = data.playlistId,
                albumSongCount = data.albumSongCount,
                continuation = data.continuation,
                firstTimeLoaded = data.firstTimeLoaded,
            )
        }

        is QueueType.LOCAL_ALBUM_RADIO -> {
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position,
            )
        }
    }
