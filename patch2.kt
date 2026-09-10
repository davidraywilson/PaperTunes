        setContent {
            PaperUITheme {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    androidx.compose.material3.Surface(
                        color = androidx.compose.ui.graphics.Color.White,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ) {
                        CompositionLocalProvider(
                            LocalDatabase provides database,
                            LocalDownloadUtil provides downloadUtil,
                            LocalSyncUtils provides syncUtils,
                            LocalPlayerConnection provides playerConnection,
                            LocalPlayerAwareWindowInsets provides WindowInsets.systemBars,
                            androidx.compose.material3.LocalContentColor provides androidx.compose.ui.graphics.Color.Black,
                        ) {
                            EinkApp()
                        }
                    }
                }
            }
        }
