package moe.rukamori.papertunes

import kotlinx.coroutines.runBlocking
import org.junit.Test
import moe.rukamori.papertunes.innertube.YouTube
import moe.rukamori.papertunes.utils.YTPlayerUtils
import moe.rukamori.papertunes.constants.AudioQuality

class DownloadTest {
    @Test
    fun testDownloadStream() = runBlocking {
        try {
            // Using a dummy video ID, e.g. a popular song "dQw4w9WgXcQ"
            val response = YTPlayerUtils.playerResponseForDownload(
                videoId = "dQw4w9WgXcQ",
                audioQuality = AudioQuality.AUTO,
                connectivityManager = null!!, // This might crash if it needs context, let's see
                networkMetered = false
            )
            println("SUCCESS: " + response.getOrThrow().streamUrl)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
