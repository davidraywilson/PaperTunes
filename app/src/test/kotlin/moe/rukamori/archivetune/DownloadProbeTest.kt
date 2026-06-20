package moe.rukamori.archivetune

import kotlinx.coroutines.runBlocking
import org.junit.Test
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.utils.YTPlayerUtils
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.innertube.models.YouTubeClient

class DownloadProbeTest {
    @Test
    fun testDownloadStream() = runBlocking {
        try {
            val response = YTPlayerUtils.playerResponseForPlayback(
                videoId = "dQw4w9WgXcQ",
                playlistId = null,
                audioQuality = AudioQuality.LOW,
                connectivityManager = null,
                preferredStreamClient = moe.rukamori.archivetune.constants.PlayerStreamClient.IOS,
                networkMetered = true
            )
            println("SUCCESS STREAM URL: " + response.getOrThrow().streamUrl)
        } catch (e: Exception) {
            println("FAILED STREAM URL")
            e.printStackTrace()
        }
    }
}
