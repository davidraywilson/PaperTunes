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
 * Owned by the `settings` child agent. Replace these stub bodies with the real e-ink
 * More + Settings screens. Keep the composable names and signatures identical (EinkApp's
 * NavHost references them). You may add more files in this package; just ensure each
 * composable below is defined exactly once across the module.
 */

@Composable
fun EinkMoreScreen(navController: NavController) =
    EinkEmptyState("More", "Coming soon.", Modifier.fillMaxSize())

@Composable
fun EinkSettingsScreen(navController: NavController) =
    EinkEmptyState("Settings", "Coming soon.", Modifier.fillMaxSize())
