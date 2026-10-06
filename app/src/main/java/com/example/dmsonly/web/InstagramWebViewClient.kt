package com.example.dmsonly.web

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.dmsonly.web.InstagramRoutes.Decision

class InstagramWebViewClient(
    dopamineFreeUi: Boolean = false,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onMainFrameError: (String) -> Unit,
    private val onMainFrameRecovered: () -> Unit,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {
    private val dmReelNavigationGuard = DmReelNavigationGuard()
    @Volatile
    private var dopamineFreeUiEnabled = dopamineFreeUi
    fun setDopamineFreeUi(enabled: Boolean) {
        dopamineFreeUiEnabled = enabled
    }

    /**
     * Receives modern Jetpack WebKit navigation callbacks, including
     * same-document SPA navigations such as history.pushState/replaceState.
     *
     * This is a second line of defense behind the synchronous page-side
     * history hook in InstagramUiShield.
     */
    fun handleNavigationStarted(view: WebView, url: String) {
        dmReelNavigationGuard.redirectTarget(url)?.let { returnTo ->
            if (returnTo != url) view.loadUrl(returnTo)
            return
        }

        if (InstagramRoutes.isBlocked(url) && !InstagramRoutes.isInboxUrl(view.url)) {
            view.loadUrl(InstagramRoutes.DM_INBOX)
        }
    }
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        dmReelNavigationGuard.redirectTarget(view.url)
        dmReelNavigationGuard.redirectTarget(request.url.toString())?.let { returnTo ->
            view.loadUrl(returnTo)
            return true
        }
        return when (val decision = InstagramRoutes.decide(request.url)) {
            Decision.Allow -> false
            Decision.Block -> true
            is Decision.Redirect -> {
                if (!InstagramRoutes.isInboxUrl(view.url)) view.loadUrl(decision.url)
                true
            }
            is Decision.OpenExternally -> {
                openExternal(view, Uri.parse(decision.url))
                true
            }
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        dmReelNavigationGuard.redirectTarget(url)?.let { returnTo ->
            view.loadUrl(returnTo)
            return
        }
        if (InstagramRoutes.isBlocked(url) && !InstagramRoutes.isInboxUrl(view.url)) {
            view.loadUrl(InstagramRoutes.DM_INBOX)
        }
        super.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        val history = view.copyBackForwardList()
        dmReelNavigationGuard.restoreFromHistory(
            (0..history.currentIndex).map { history.getItemAtIndex(it).url },
            history.currentIndex
        )
        dmReelNavigationGuard.redirectTarget(url)?.let { returnTo ->
            view.loadUrl(returnTo)
            return
        }
        onLoadingChanged(true)
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (runCatching { InstagramRoutes.isInstagramHost(Uri.parse(url ?: "")) }.getOrDefault(false)) {
            InstagramUiShield.install(view, dopamineFreeUiEnabled)
        }
        onLoadingChanged(false)
        onMainFrameRecovered()
        super.onPageFinished(view, url)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) {
            onLoadingChanged(false)
            onMainFrameError(error.description?.toString() ?: "Unable to connect to Instagram")
        }
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame && errorResponse.statusCode >= 400) {
            onLoadingChanged(false)
            onMainFrameError("Instagram returned HTTP " + errorResponse.statusCode + ". Try again in a moment.")
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        if (error.url == view.url) {
            onLoadingChanged(false)
            onMainFrameError("The secure connection to Instagram could not be verified.")
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onLoadingChanged(false)
        onRendererGone()
        return true
    }

    private fun openExternal(view: WebView, uri: Uri) {
        try {
            view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            onMainFrameError("No app can open this link.")
        }
    }
}
