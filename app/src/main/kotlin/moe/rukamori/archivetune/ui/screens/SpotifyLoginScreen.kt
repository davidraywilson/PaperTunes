/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.spotify.SpotifyAccountViewModel
import moe.rukamori.archivetune.spotify.SpotifyAuth
import moe.rukamori.archivetune.spotify.SpotifyLoginWebChromeClient
import moe.rukamori.archivetune.spotify.configureSpotifyLoginWebView
import moe.rukamori.archivetune.spotify.destroySpotifyLoginWebView
import moe.rukamori.archivetune.spotify.readSpotifyCookies
import moe.rukamori.archivetune.spotify.shouldOverrideSpotifyLoginUrl
import moe.rukamori.archivetune.utils.resetAuthWebViewSession

const val SPOTIFY_LOGIN_ROUTE = "spotify_login"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLoginScreen(
    navController: NavController,
    onLoginComplete: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SpotifyAccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroySpotifyLoginWebView()
            mainWebView?.takeIf { it !== webView }?.destroySpotifyLoginWebView()
            webView = null
            mainWebView = null
        }
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            Toast.makeText(context, "Logged in to Spotify", Toast.LENGTH_SHORT).show()
            if (onLoginComplete != null) {
                onLoginComplete()
            } else {
                navController.navigateUp()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    BackHandler(enabled = true) {
        val activeWebView = webView
        val rootWebView = mainWebView
        when {
            activeWebView?.canGoBack() == true -> {
                activeWebView.goBack()
            }
            activeWebView != null && rootWebView != null && activeWebView !== rootWebView -> {
                activeWebView.destroySpotifyLoginWebView()
                webView = rootWebView
            }
            else -> {
                if (onNavigateBack != null) onNavigateBack() else navController.navigateUp()
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        contentColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Spotify Login", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (onNavigateBack != null) onNavigateBack() else navController.navigateUp()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            tint = Color.Black,
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    trackColor = Color.LightGray,
                )
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    val container = FrameLayout(ctx)
                    val spotifyWebView = WebView(ctx).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        configureSpotifyLoginWebView()

                        fun captureCookies(url: String?): Boolean {
                            if (captured) return true
                            val cookies = readSpotifyCookies(cookieManager, url)
                            val spDc = cookies["sp_dc"].orEmpty()
                            if (spDc.isBlank()) return false
                            captured = true
                            cookieManager.flush()
                            viewModel.connectWithCookies(spDc, cookies["sp_key"].orEmpty())
                            return true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean =
                                shouldOverrideSpotifyLoginUrl(
                                    view = view,
                                    url = request.url?.toString(),
                                    captureCookies = { targetUrl -> captureCookies(targetUrl) },
                                )

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String?,
                            ): Boolean =
                                shouldOverrideSpotifyLoginUrl(
                                    view = view,
                                    url = url,
                                    captureCookies = { targetUrl -> captureCookies(targetUrl) },
                                )

                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: android.graphics.Bitmap?,
                            ) {
                                captureCookies(url)
                            }

                            override fun onPageFinished(
                                view: WebView,
                                url: String?,
                            ) {
                                captureCookies(url)
                            }
                        }

                        webChromeClient = SpotifyLoginWebChromeClient(
                            container = container,
                            parentWebView = this,
                            captureCookies = { url -> captureCookies(url) },
                            onActiveWebViewChanged = { active -> webView = active },
                        )

                        webView = this
                        mainWebView = this
                        resetAuthWebViewSession(ctx, this) {
                            loadUrl(SpotifyAuth.LOGIN_URL)
                        }
                    }
                    container.addView(
                        spotifyWebView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    container
                },
                update = {
                    webView = webView ?: mainWebView
                }
            )
        }
    }
}
