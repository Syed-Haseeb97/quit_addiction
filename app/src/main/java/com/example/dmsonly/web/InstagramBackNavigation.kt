package com.example.dmsonly.web

import android.webkit.WebView

/**
 * Keeps browser history useful without allowing Feed/Explore/Reels to become destinations.
 */
object InstagramBackNavigation {
    fun goBack(webView: WebView): Boolean {
        if (InstagramRoutes.isInboxUrl(webView.url)) return false

        val history = webView.copyBackForwardList()
        val urls = (0..history.currentIndex).map { history.getItemAtIndex(it).url }
        val target = previousAllowedHistoryIndex(urls, history.currentIndex)
        if (target != null) {
            webView.goBackOrForward(target - history.currentIndex)
            return true
        }

        if (!InstagramRoutes.isAuthFlowUrl(webView.url)) {
            webView.loadUrl(InstagramRoutes.DM_INBOX)
            return true
        }

        return false
    }

    internal fun previousAllowedHistoryIndex(urls: List<String>, currentIndex: Int): Int? {
        for (index in currentIndex - 1 downTo 0) {
            if (!InstagramRoutes.isBlocked(urls[index])) return index
        }
        return null
    }
}
