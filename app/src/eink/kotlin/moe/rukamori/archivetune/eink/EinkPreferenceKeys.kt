package moe.rukamori.archivetune.eink

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

val AutoDownloadPlaylistsKey = stringSetPreferencesKey("eink_auto_download_playlists")
val EinkOnboardingCompletedKey = booleanPreferencesKey("eink_onboarding_completed")
