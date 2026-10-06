package com.example.dmsonly.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramWebView(
    modifier: Modifier = Modifier,
    reloadToken: Int = 0,
    onWebViewReady: (WebView) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onRecovered: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(
            if (uris.isEmpty()) null else uris.toTypedArray()
        )
        pendingFileCallback = null
    }

    val singleFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingFileCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        pendingFileCallback = null
    }

    fun launchFilePicker(
        acceptTypes: Array<String>,
        allowMultiple: Boolean,
        callback: ValueCallback<Array<Uri>>?
    ) {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = callback

        val normalized = acceptTypes
            .flatMap { it.split(',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val mime = when {
            normalized.isEmpty() -> "*/*"
            normalized.any { it.startsWith("image/") || it.startsWith("video/") } -> "*/*"
            else -> normalized.first()
        }

        if (allowMultiple) {
            filePicker.launch(arrayOf(mime))
        } else {
            singleFilePicker.launch(arrayOf(mime))
        }
    }

    LaunchedEffect(reloadToken) {
        if (reloadToken > 0) webView?.reload()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = FrameLayout.LayoutParams(
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
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    // Intentionally retain the current WebView-provided UA.
                    // Do not hard-code an old Chrome UA.
                }

                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(this@apply, true)
                }

                webViewClient = InstagramWebViewClient(
                    onLoadingChanged = onLoadingChanged,
                    onMainFrameError = onError,
                    onMainFrameRecovered = onRecovered
                )

                webChromeClient = InstagramWebChromeClient(::launchFilePicker)

                if (savedInstanceStateForWebView == null) {
                    loadUrl(InstagramRoutes.DM_INBOX)
                }

                webView = this
                onWebViewReady(this)
            }
        },
        update = {
            webView = it
            onWebViewReady(it)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { view ->
                view.stopLoading()
                view.webChromeClient = null
                view.webViewClient = null
                view.destroy()
            }
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
        }
    }
}

// Kept as a simple process-local hook; MainActivity does not need to persist
// arbitrary WebView state or credentials.
private var savedInstanceStateForWebView: Bundle? = null

fun clearInstagramSession(webView: WebView?, onComplete: () -> Unit) {
    if (webView == null) {
        onComplete()
        return
    }

    webView.stopLoading()
    webView.clearHistory()
    webView.clearCache(true)

    CookieManager.getInstance().removeAllCookies {
        CookieManager.getInstance().flush()
        webView.loadUrl(InstagramRoutes.DM_INBOX)
        onComplete()
    }
}
