package moe.rukamori.archivetune.adaptors

import moe.rukamori.archivetune.playback.DownloadUtil

interface PlaybackAdaptor {
    fun shouldPreferDownload(mediaId: String, downloadUtil: DownloadUtil): Boolean
}

object PlaybackAdaptorRegistry {
    var current: PlaybackAdaptor = object : PlaybackAdaptor {
        override fun shouldPreferDownload(mediaId: String, downloadUtil: DownloadUtil): Boolean = false
    }
}
