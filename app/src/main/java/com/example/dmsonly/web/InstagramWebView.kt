package com.example.dmsonly.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramWebView(
    modifier: Modifier = Modifier,
    initialWebViewState: Bundle? = null,
    reloadToken: Int = 0,
    darkAppearance: Boolean,
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
    val lifecycleOwner = (androidx.compose.ui.platform.LocalContext.current as ComponentActivity)

    val chromeClient = remember {
        InstagramWebChromeClient { latestOnLoading(it < 100) }
    }

    val chooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        chromeClient.deliverFileChooserResult(result.resultCode, result.data)
    }

    SideEffect {
        chromeClient.launchChooser = { intent -> chooserLauncher.launch(intent) }
        webView?.let { applyWebAppearance(it, darkAppearance) }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView?.onPause()
                    CookieManager.getInstance().flush()
                }
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

                    if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }

                    applyWebAppearance(this, darkAppearance)

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
                        setGeolocationEnabled(false)
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Keep the installed WebView's current User-Agent; never hard-code an old UA.
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    // Instagram login/session behavior can depend on third-party cookies.
                    // Keep this enabled for compatibility; no credentials are stored by the app.
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = InstagramWebViewClient(
                        onLoadingChanged = latestOnLoading,
                        onMainFrameError = latestOnError,
                        onMainFrameRecovered = latestOnRecovered,
                        onRendererGone = {
                            chromeClient.cancelPendingFileChooser()
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
                chromeClient.cancelPendingFileChooser()
                it.stopLoading()
                it.destroy()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            chromeClient.cancelPendingFileChooser()
            webView?.let {
                it.stopLoading()
                it.destroy()
            }
            webView = null
        }
    }
}

private fun applyWebAppearance(webView: WebView, darkAppearance: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        WebSettingsCompat.setForceDark(
            webView.settings,
            if (darkAppearance) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
        )
    }
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, darkAppearance)
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
    webView.loadUrl("about:blank")
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
