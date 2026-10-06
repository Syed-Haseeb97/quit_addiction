package com.example.dmsonly.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramWebView(
    modifier: Modifier = Modifier,
    initialWebViewState: Bundle? = null,
    reloadToken: Int = 0,
    onWebViewReady: (WebView) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onRecovered: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var generation by remember { mutableIntStateOf(0) }

    val latestOnLoading by androidx.compose.runtime.rememberUpdatedState(onLoadingChanged)
    val latestOnError by androidx.compose.runtime.rememberUpdatedState(onError)
    val latestOnRecovered by androidx.compose.runtime.rememberUpdatedState(onRecovered)

    val chromeClient = remember {
        InstagramWebChromeClient { latestOnLoading(it < 100) }
    }

    val chooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        chromeClient.deliverFileChooserResult(result.resultCode, result.data)
    }

    androidx.compose.runtime.SideEffect {
        chromeClient.launchChooser = { intent -> chooserLauncher.launch(intent) }
    }

    LaunchedEffect(reloadToken) {
        if (reloadToken > 0) webView?.reload()
    }

    key(generation) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        mediaPlaybackRequiresUserGesture = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Keep the WebView-provided User-Agent; never hard-code an old UA.
                    }

                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(this@apply, true)
                    }

                    webViewClient = InstagramWebViewClient(
                        onLoadingChanged = latestOnLoading,
                        onMainFrameError = latestOnError,
                        onMainFrameRecovered = latestOnRecovered,
                        onRendererGone = {
                            latestOnError("Instagram's WebView process stopped. Recreating the WebView…")
                            generation++
                        }
                    )
                    webChromeClient = chromeClient

                    installDocumentStartShieldIfSupported(this)

                    if (generation == 0 && initialWebViewState != null) {
                        restoreState(initialWebViewState)
                    } else {
                        loadUrl(InstagramRoutes.DM_INBOX)
                    }

                    webView = this
                    onWebViewReady(this)
                }
            },
            update = {
                webView = it
                onWebViewReady(it)
            },
            onRelease = {
                it.stopLoading()
                it.webChromeClient = null
                it.webViewClient = null
                it.destroy()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webView?.let {
                it.stopLoading()
                it.webChromeClient = null
                it.webViewClient = null
                it.destroy()
            }
            webView = null
        }
    }
}

private fun installDocumentStartShieldIfSupported(webView: WebView) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            InstagramUiShield.script,
            setOf("https://instagram.com", "https://*.instagram.com")
        )
    }
}

fun clearInstagramSession(webView: WebView?, onComplete: () -> Unit) {
    if (webView == null) {
        onComplete()
        return
    }

    webView.stopLoading()
    webView.clearHistory()
    webView.clearCache(true)
    webView.clearFormData()
    WebStorage.getInstance().deleteAllData()

    CookieManager.getInstance().removeAllCookies {
        CookieManager.getInstance().flush()
        webView.loadUrl(InstagramRoutes.DM_INBOX)
        onComplete()
    }
}
