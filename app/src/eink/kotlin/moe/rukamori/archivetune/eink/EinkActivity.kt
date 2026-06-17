/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.mudita.mmd.ThemeMMD
import dagger.hilt.android.AndroidEntryPoint
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.LocalSyncUtils
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.playback.DownloadUtil
import moe.rukamori.archivetune.playback.MusicService
import moe.rukamori.archivetune.playback.MusicService.MusicBinder
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.utils.SyncUtils
import moe.rukamori.archivetune.utils.reportException
import javax.inject.Inject

/**
 * Launcher Activity for the new e-ink (MMD) UI. Reuses ArchiveTune's backend: it binds the
 * shared [MusicService], builds a [PlayerConnection], and provides the same composition
 * locals the legacy UI relies on, then hosts [EinkApp] under the MMD theme.
 */
@AndroidEntryPoint
class EinkActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isMusicServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isMusicServiceBound = true
            if (service is MusicBinder) {
                playerConnection = PlayerConnection(this@EinkActivity, service, database, lifecycleScope)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isMusicServiceBound = false
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        isMusicServiceBound = bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
    }

    override fun onStop() {
        if (isMusicServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                // Service was not bound; ignore.
            } catch (e: Exception) {
                reportException(e)
            } finally {
                isMusicServiceBound = false
            }
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                // Fix for Android 16 API 36 Color.Unspecified crash:
                // ThemeMMD might leave surface color unspecified, crashing Material 3 TopAppBar.
                androidx.compose.material3.MaterialTheme(
                    colorScheme = androidx.compose.material3.lightColorScheme(
                        surface = androidx.compose.ui.graphics.Color.White,
                        background = androidx.compose.ui.graphics.Color.White,
                        onSurface = androidx.compose.ui.graphics.Color.Black,
                        onBackground = androidx.compose.ui.graphics.Color.Black,
                        surfaceTint = androidx.compose.ui.graphics.Color.White,
                        primary = androidx.compose.ui.graphics.Color.Black,
                        onPrimary = androidx.compose.ui.graphics.Color.White,
                        primaryContainer = androidx.compose.ui.graphics.Color.Black,
                        onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
                        secondary = androidx.compose.ui.graphics.Color.White,
                        onSecondary = androidx.compose.ui.graphics.Color.Black,
                        secondaryContainer = androidx.compose.ui.graphics.Color.Black,
                        onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
                        tertiary = androidx.compose.ui.graphics.Color.Black,
                        onTertiary = androidx.compose.ui.graphics.Color.White,
                        tertiaryContainer = androidx.compose.ui.graphics.Color.Black,
                        onTertiaryContainer = androidx.compose.ui.graphics.Color.White,
                        surfaceVariant = androidx.compose.ui.graphics.Color.White,
                        onSurfaceVariant = androidx.compose.ui.graphics.Color.Black,
                        outline = androidx.compose.ui.graphics.Color.Black,
                        outlineVariant = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalDownloadUtil provides downloadUtil,
                        LocalSyncUtils provides syncUtils,
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides WindowInsets.systemBars,
                    ) {
                        EinkApp()
                    }
                }
            }
        }
    }
}
