package com.example.dmsonly.web

import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class InstagramWebViewClient(
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onMainFrameError: (String) -> Unit,
    private val onMainFrameRecovered: () -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        if (!request.isForMainFrame) return false

        val uri = request.url
        return when {
            InstagramRoutes.isInstagramHost(uri) -> {
                if (InstagramRoutes.isBlocked(uri)) {
                    view.loadUrl(InstagramRoutes.DM_INBOX)
                    true
                } else {
                    false
                }
            }

            uri.scheme == "http" || uri.scheme == "https" -> {
                openExternal(view, uri)
                true
            }

            else -> true
        }
    }

    override fun onPageStarted(
        view: WebView,
        url: String?,
        favicon: android.graphics.Bitmap?
    ) {
        onLoadingChanged(true)
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        onLoadingChanged(false)
        InstagramUiShield.install(view)
        onMainFrameRecovered()
        super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError
    ) {
        if (request.isForMainFrame) {
            onLoadingChanged(false)
            onMainFrameError("Unable to connect to Instagram")
        }
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        if (request.isForMainFrame && errorResponse.statusCode >= 400) {
            onLoadingChanged(false)
            onMainFrameError(
                "Instagram returned an HTTP " + errorResponse.statusCode + " error"
            )
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        // Always reject certificate errors. Never call handler.proceed().
        handler.cancel()

        if (error.url == view.url) {
            onLoadingChanged(false)
            onMainFrameError("Secure connection to Instagram could not be verified")
        }
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: android.webkit.RenderProcessGoneDetail
    ): Boolean {
        onLoadingChanged(false)
        onMainFrameError("Instagram's WebView process stopped. Restart the app to continue.")
        return true
    }

    private fun openExternal(view: WebView, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        runCatching { view.context.startActivity(intent) }
    }
}
