/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mudita.mmd.components.lazy.LazyColumnMMD
import moe.rukamori.archivetune.App.Companion.forgetAccount
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.eink.EinkScreen
import moe.rukamori.archivetune.innertube.utils.hasYouTubeLoginCookie
import moe.rukamori.archivetune.ui.screens.buildLoginRoute
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.eink.components.EinkEmptyState
import moe.rukamori.archivetune.eink.components.EinkTwoLineRow

/*
 * Owned by the `settings` child agent. Replace these stub bodies with the real e-ink
 * More + Settings screens. Keep the composable names and signatures identical (EinkApp's
 * NavHost references them). You may add more files in this package; just ensure each
 * composable below is defined exactly once across the module.
 */

@Composable
fun EinkMoreScreen(navController: NavController) {
    LazyColumnMMD(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            EinkTwoLineRow(
                title = "Settings",
                subtitle = "App preferences and options",
                onClick = { navController.navigate(EinkScreen.Settings.route) },
                showDivider = false
            )
        }
    }
}

@Composable
fun EinkSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = hasYouTubeLoginCookie(innerTubeCookie)

    LazyColumnMMD(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            EinkTwoLineRow(
                title = "YouTube Login",
                subtitle = if (isLoggedIn) "Logged in (Tap to logout)" else "Not logged in (Tap to login)",
                onClick = {
                    if (isLoggedIn) {
                        onInnerTubeCookieChange("")
                        forgetAccount(context, clearWebAuthSession = true)
                    } else {
                        navController.navigate(buildLoginRoute())
                    }
                },
                showDivider = false
            )
        }
    }
}
