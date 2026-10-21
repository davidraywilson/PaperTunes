package moe.rukamori.archivetune.eink.adaptors

import androidx.media3.exoplayer.offline.Download
import moe.rukamori.archivetune.adaptors.PlaybackAdaptor
import moe.rukamori.archivetune.playback.DownloadUtil

class EinkPlaybackAdaptor : PlaybackAdaptor {
    override fun shouldPreferDownload(mediaId: String, downloadUtil: DownloadUtil): Boolean {
        return downloadUtil.downloads.value[mediaId]?.state == Download.STATE_COMPLETED
    }
}
