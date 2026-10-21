/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import moe.rukamori.archivetune.canvas.models.CanvasArtwork

@Immutable
data class CanvasPlaybackRequest(
    val mediaId: String,
    val title: String,
    val artist: String,
    val storefront: String,
    val requireVertical: Boolean,
)

class CanvasPlaybackUseCase @Inject constructor(
    private val repository: CanvasArtworkRepository,
    private val offlineCanvasStore: OfflineCanvasStore,
    private val downloadUtil: moe.rukamori.archivetune.playback.DownloadUtil,
) {
    val policy = CanvasNetworkAccess.policy
    val revision = repository.revision
    val spotifyConnected = repository.spotifyConnected

    /**
     * Resolution order:
     * 1. A canvas saved for the song (database) or already in the playback cache: play it. No lookup.
     * 2. Otherwise, if network is allowed (Wi-Fi, or mobile data with "Wi-Fi only" disabled): resolve and
     *    stream it. This also caches it, and saves it for downloaded songs that don't have one yet.
     * 3. Otherwise null.
     */
    suspend fun load(request: CanvasPlaybackRequest, policy: CanvasPolicy): CanvasVideo? {
        if (policy.ready && !policy.configuration.enabled) return null

        val offlineFile = offlineCanvasStore.find(request.mediaId)
        if (offlineFile != null) {
            checkForNewerInBackground(request, policy)
            val fileUrl = android.net.Uri.fromFile(offlineFile).toString()
            return CanvasVideo(
                source = policy.configuration.source.takeUnless { it == CanvasSource.ALL } ?: CanvasSource.SPOTIFY,
                static = null,
                animated = fileUrl,
                videoUrl = fileUrl,
                animatedVertical = fileUrl,
                videoUrlVertical = fileUrl,
            )
        }

        val cachedArtwork = moe.rukamori.archivetune.ui.player.CanvasArtworkPlaybackCache.get(request.mediaId, preferCachedOnly = true)
        val cachedLocalUrl = cachedArtwork?.let { it.videoUrl ?: it.animated }
        if (cachedArtwork != null && cachedLocalUrl != null && cachedLocalUrl.startsWith("file:", ignoreCase = true)) {
            val shouldPreferSpotify = spotifyConnected.first() && policy.configuration.source.accepts(CanvasSource.SPOTIFY)
            if (!shouldPreferSpotify || cachedArtwork.source == CanvasSource.SPOTIFY || !policy.networkAllowed) {
                // Save the cached file to the database for downloaded songs.
                downloadUtil.ensureOfflineCanvas(request.mediaId, cachedArtwork)
                checkForNewerInBackground(request, policy)
                return CanvasVideo(
                    source = cachedArtwork.source ?: (policy.configuration.source.takeUnless { it == CanvasSource.ALL } ?: CanvasSource.SPOTIFY),
                    static = null,
                    animated = cachedArtwork.animated ?: cachedLocalUrl,
                    videoUrl = cachedArtwork.videoUrl ?: cachedLocalUrl,
                    animatedVertical = cachedArtwork.animatedVertical ?: cachedLocalUrl,
                    videoUrlVertical = cachedArtwork.videoUrlVertical ?: cachedLocalUrl,
                )
            }
        }

        if (!policy.ready || !policy.networkAllowed) return null

        val artwork = repository.resolve(request, policy) ?: return null
        val source = artwork.source ?: return null
        downloadUtil.ensureOfflineCanvas(request.mediaId, artwork)
        return CanvasVideo(
            source = source,
            static = artwork.static,
            animated = artwork.animated,
            videoUrl = artwork.videoUrl,
            animatedVertical = artwork.animatedVertical,
            videoUrlVertical = artwork.videoUrlVertical,
        )
    }

    /**
     * While a local canvas plays, quietly look for a newer one (once per song per app run, only when
     * network is allowed). If the provider's video differs from the one we hold, force-refreshes it.
     * The existing offline video remains safe and playable until the replacement is downloaded.
     */
    private fun checkForNewerInBackground(request: CanvasPlaybackRequest, policy: CanvasPolicy) {
        if (!policy.ready || !policy.networkAllowed) return
        if (!checkedForUpdate.add(request.mediaId)) return
        backgroundScope.launch {
            try {
                val cache = moe.rukamori.archivetune.ui.player.CanvasArtworkPlaybackCache
                val known = cache.originalArtwork(request.mediaId) ?: return@launch
                val latest = repository.resolve(request, policy, streamLatest = true) ?: return@launch
                fun CanvasArtwork.videoKey() = videoUrl ?: animated ?: videoUrlVertical ?: animatedVertical
                val latestKey = latest.videoKey() ?: return@launch
                if (latestKey == known.videoKey()) return@launch
                repository.resolve(request, policy, forceRefresh = true)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                timber.log.Timber.w(error, "Canvas update check failed for %s", request.mediaId)
                checkedForUpdate.remove(request.mediaId)
            }
        }
    }

    private companion object {
        val checkedForUpdate: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
        val backgroundScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
        )
    }

    suspend fun refresh(request: CanvasPlaybackRequest): Boolean = coroutineScope {
        val current = policy.value
        if (!current.networkAllowed) return@coroutineScope false
        val work = async { repository.resolve(request, current, forceRefresh = true) != null }
        val watcher = launch {
            policy.first { it != current }
            work.cancel()
        }
        try {
            work.await()
        } finally {
            watcher.cancel()
        }
    }
}
