/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.ui.player.CanvasArtworkPlaybackCache
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the canvas videos saved alongside downloaded songs (`song.offlineCanvasUrl`).
 *
 * Files live in their own directory under `filesDir`. They must NOT be placed in the ExoPlayer
 * download cache directory: `SimpleCache` deletes any file it doesn't recognise as a cache span
 * when it initialises, which silently wiped offline canvases on every app start.
 */
@Singleton
class OfflineCanvasStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val locks = ConcurrentHashMap<String, Mutex>()

    private val directory: File
        get() = context.filesDir.resolve(DIRECTORY_NAME).apply { mkdirs() }

    private fun fileFor(mediaId: String): File =
        directory.resolve("canvas_${mediaId.replace(UnsafeFileChars, "_")}.mp4")

    /** Returns the saved offline canvas for [mediaId], or null when none exists on disk. */
    suspend fun find(mediaId: String): File? = withContext(Dispatchers.IO) {
        if (mediaId.isBlank()) return@withContext null
        val storedPath = database.getOfflineCanvasUrlBlocking(mediaId)
        if (!storedPath.isNullOrBlank()) {
            val file = if (storedPath.startsWith("file:", ignoreCase = true)) {
                runCatching { File(Uri.parse(storedPath).path ?: storedPath) }.getOrNull()
            } else {
                File(storedPath)
            }
            if (file != null && file.exists() && file.length() > 0L) {
                return@withContext file
            }
        }
        val canonical = fileFor(mediaId)
        if (canonical.exists() && canonical.length() > 0L) {
            setStoredPath(mediaId, canonical.absolutePath)
            return@withContext canonical
        }
        null
    }

    /**
     * Ensures a downloaded song has an offline canvas. Prefers copying an already-cached local
     * video; only hits the network (policy-guarded, so Wi-Fi only is respected) when the playback
     * cache is disabled and therefore will never produce a file to copy.
     *
     * When the playback cache is enabled and still downloading, this is a no-op; the copy happens
     * through [saveFromFile] once [CanvasArtworkPlaybackCache.artworkCachedEvent] fires.
     */
    suspend fun persist(mediaId: String, artwork: CanvasArtwork?, overwrite: Boolean = false) = withContext(Dispatchers.IO) {
        if (mediaId.isBlank()) return@withContext
        lockFor(mediaId).withLock {
            if (!overwrite && find(mediaId) != null) return@withLock
            Timber.tag("Canvas").d("persist(%s): artwork=%s, cached=%s", mediaId, artwork != null, CanvasArtworkPlaybackCache.get(mediaId, preferCachedOnly = true) != null)
            val local = artwork?.localFile() ?: CanvasArtworkPlaybackCache.get(mediaId, preferCachedOnly = true)?.localFile()
            if (local != null) {
                copyLocked(mediaId, local)
                return@withLock
            }
            // If the playback cache is currently caching this artwork in the background, await it
            val cacheJob = CanvasArtworkPlaybackCache.getActiveCacheJob(mediaId)
            if (cacheJob != null) {
                runCatching { cacheJob.join() }
                val cachedLocal = CanvasArtworkPlaybackCache.get(mediaId, preferCachedOnly = true)?.localFile()
                if (cachedLocal != null) {
                    copyLocked(mediaId, cachedLocal)
                    return@withLock
                }
            }
            if (artwork == null) return@withLock
            if (!CanvasNetworkAccess.policy.value.networkAllowed) return@withLock
            val target = fileFor(mediaId)
            if (CanvasArtworkPlaybackCache.downloadForOffline(artwork, target)) {
                setStoredPath(mediaId, target.absolutePath)
            }
        }
    }

    /** Copies an already-downloaded canvas video into offline storage if none is saved yet (or if overwriting). */
    suspend fun saveFromFile(mediaId: String, source: File, overwrite: Boolean = false) = withContext(Dispatchers.IO) {
        if (mediaId.isBlank() || !source.exists() || source.length() <= 0L) return@withContext
        lockFor(mediaId).withLock {
            if (overwrite || find(mediaId) == null) copyLocked(mediaId, source)
        }
    }

    suspend fun remove(mediaId: String) = withContext(Dispatchers.IO) {
        if (mediaId.isBlank()) return@withContext
        lockFor(mediaId).withLock {
            try {
                database.getOfflineCanvasUrlBlocking(mediaId)?.let { File(it).delete() }
                fileFor(mediaId).delete()
                setStoredPath(mediaId, null)
            } catch (error: Exception) {
                Timber.w(error, "Failed to remove offline canvas for %s", mediaId)
            }
        }
    }

    private fun copyLocked(mediaId: String, source: File) {
        if (!source.exists() || source.length() <= 0L) return
        val target = fileFor(mediaId)
        try {
            source.copyTo(target, overwrite = true)
            setStoredPath(mediaId, target.absolutePath)
        } catch (error: Exception) {
            Timber.w(error, "Failed to save offline canvas for %s", mediaId)
        }
    }

    private fun setStoredPath(mediaId: String, path: String?) {
        Timber.tag("Canvas").d("setStoredPath(%s) = %s", mediaId, path)
        val existing = database.getSongByIdBlocking(mediaId)
        if (existing == null && path != null) {
            database.upsert(moe.rukamori.archivetune.db.entities.SongEntity(id = mediaId, title = mediaId, dateDownload = null, offlineCanvasUrl = path))
        } else {
            database.updateOfflineCanvasUrl(mediaId, path)
        }
    }

    private fun lockFor(mediaId: String): Mutex = locks.getOrPut(mediaId) { Mutex() }

    private fun CanvasArtwork.localFile(): File? =
        listOf(videoUrl, animated, videoUrlVertical, animatedVertical)
            .firstNotNullOfOrNull { url ->
                url?.takeIf { it.startsWith("file:", ignoreCase = true) }
                    ?.let { Uri.parse(it).path }
                    ?.let { File(it) }
                    ?.takeIf { it.exists() && it.length() > 0L }
            }

    private companion object {
        const val DIRECTORY_NAME = "offline_canvas"
        val UnsafeFileChars = Regex("[^A-Za-z0-9_-]")
    }
}
