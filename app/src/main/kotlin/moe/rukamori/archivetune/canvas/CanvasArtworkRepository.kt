/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import moe.rukamori.archivetune.canvas.models.matchesSongIdentity
import moe.rukamori.archivetune.ui.player.CanvasArtworkPlaybackCache
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanvasArtworkRepository @Inject constructor(
    private val spotifyCanvas: SpotifyCanvasRepository,
) {
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()
    val spotifyConnected = spotifyCanvas.connected

    suspend fun resolve(
        request: CanvasPlaybackRequest,
        policy: CanvasPolicy,
        forceRefresh: Boolean = false,
        streamLatest: Boolean = false,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (request.mediaId.isBlank()) return@withContext null
        val source = policy.configuration.source
        if (!forceRefresh && !streamLatest) {
            val cached = CanvasArtworkPlaybackCache.get(request.mediaId, preferCachedOnly = true)
            val shouldPreferSpotify = spotifyCanvas.connected.first() && source.accepts(CanvasSource.SPOTIFY)
            if (cached != null && source.accepts(cached.source) && cached.matches(request)) {
                if (!shouldPreferSpotify || cached.source == CanvasSource.SPOTIFY || !policy.networkAllowed) {
                    return@withContext cached.copy(static = if (policy.networkAllowed) cached.static else null)
                }
            }
        }
        val bypassWifi = !policy.configuration.wifiOnly
        if (!policy.networkAllowed) return@withContext null
        CanvasNetworkAccess.check(bypassWifi = bypassWifi)
        val songTitle = normalizeCanvasSongTitle(request.title)
        val artistName = normalizeCanvasArtistName(request.artist)
        val candidates = linkedSetOf(
            songTitle to artistName,
            request.title to artistName,
            songTitle to request.artist,
            request.title to request.artist,
        ).filter { (song, artist) -> song.isNotBlank() && artist.isNotBlank() }
        val fetched = providers.firstNotNullOfOrNull { provider ->
            if (!source.accepts(provider) || (provider == CanvasSource.TIDAL && request.requireVertical)) {
                return@firstNotNullOfOrNull null
            }
            try {
                withTimeoutOrNull(if (provider == CanvasSource.SPOTIFY) 45_000L else 20_000L) {
                    candidates.firstNotNullOfOrNull { (song, artist) ->
                        CanvasNetworkAccess.check(provider, bypassWifi = bypassWifi)
                        when (provider) {
                            CanvasSource.BETTER_LYRICS, CanvasSource.APPLE_MUSIC -> ArchiveTuneCanvas.getBySongArtist(
                                song = song,
                                artist = artist,
                                storefront = request.storefront,
                                source = provider,
                                requireVertical = request.requireVertical,
                                forceRefresh = forceRefresh,
                            )
                            CanvasSource.TIDAL -> TidalCanvasProvider.getBySongArtist(song, artist, request.storefront)
                            CanvasSource.SPOTIFY -> spotifyCanvas.getBySongArtist(song, artist)
                            CanvasSource.ALL -> error("Canvas lookup requires one provider")
                        }?.takeIf { it.matches(request) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Canvas lookup failed: %s", provider)
                null
            }
        } ?: return@withContext null
        CanvasNetworkAccess.check(fetched.source, bypassWifi = bypassWifi)
        val artwork = when {
            forceRefresh -> CanvasArtworkPlaybackCache.replace(request.mediaId, fetched)
            streamLatest -> {
                // Cache for offline/no-wifi use, but play the freshly fetched remote artwork.
                CanvasArtworkPlaybackCache.put(request.mediaId, fetched)
                fetched
            }
            else -> CanvasArtworkPlaybackCache.put(request.mediaId, fetched)
        }
        if (forceRefresh) mutableRevision.update { it + 1 }
        artwork
    }

    private companion object {
        val providers = listOf(CanvasSource.SPOTIFY, CanvasSource.BETTER_LYRICS, CanvasSource.APPLE_MUSIC, CanvasSource.TIDAL)
    }
}

private fun CanvasArtwork.matches(request: CanvasPlaybackRequest): Boolean =
    matchesSongIdentity(request.title, request.artist) &&
        !(if (request.requireVertical) preferredVerticalAnimationUrl else preferredAnimationUrl).isNullOrBlank()

private fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            ).replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            ).replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            ).replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull()
            .orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}
