/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import moe.rukamori.archivetune.eink.components.EinkEmptyState

/*
 * Owned by the `search` child agent. Replace this stub body with the real e-ink search
 * screen. Keep the composable name and signature identical (EinkApp's NavHost references
 * it). You may add more files in this package; just ensure EinkSearchScreen is defined
 * exactly once across the module.
 */

@Composable
fun EinkSearchScreen(navController: NavController) =
    EinkEmptyState("Search", "Coming soon.", Modifier.fillMaxSize())
