    TopAppBar(
        title = { Text(stringResource(R.string.login), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            scrolledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            navigationIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        ),
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            }
        },
    )
