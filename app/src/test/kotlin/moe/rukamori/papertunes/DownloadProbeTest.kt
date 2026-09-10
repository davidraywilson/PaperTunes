package moe.rukamori.papertunes

import kotlinx.coroutines.runBlocking
import org.junit.Test
import moe.rukamori.papertunes.innertube.YouTube
import moe.rukamori.papertunes.utils.YTPlayerUtils
import moe.rukamori.papertunes.constants.AudioQuality
import moe.rukamori.papertunes.innertube.models.YouTubeClient

class DownloadProbeTest {
    @Test
    fun testDownloadStream() = runBlocking {
        try {
            val response = YTPlayerUtils.playerResponseForPlayback(
                videoId = "dQw4w9WgXcQ",
                playlistId = null,
                audioQuality = AudioQuality.LOW,
                connectivityManager = null,
                preferredStreamClient = moe.rukamori.papertunes.constants.PlayerStreamClient.IOS,
                networkMetered = true
            )
            println("SUCCESS STREAM URL: " + response.getOrThrow().streamUrl)
        } catch (e: Exception) {
            println("FAILED STREAM URL")
            e.printStackTrace()
        }
    }
}
