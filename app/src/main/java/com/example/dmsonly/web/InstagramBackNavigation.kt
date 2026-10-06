package com.example.dmsonly.web

import android.webkit.WebView

/**
 * Keeps browser history useful without allowing Feed/Explore/Reels to become destinations.
 */
object InstagramBackNavigation {
    fun goBack(webView: WebView): Boolean {
        if (InstagramRoutes.isInboxUrl(webView.url)) return false

        val history = webView.copyBackForwardList()
        var target = history.currentIndex - 1

        while (target >= 0) {
            val url = history.getItemAtIndex(target).url
            if (!InstagramRoutes.isBlocked(url)) {
                webView.goBackOrForward(target - history.currentIndex)
                return true
            }
            target--
        }

        if (!InstagramRoutes.isAuthFlowUrl(webView.url)) {
            webView.loadUrl(InstagramRoutes.DM_INBOX)
            return true
        }

        return false
    }
}
