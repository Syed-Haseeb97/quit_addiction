package com.example.dmsonly.web

import java.net.URI

internal class DmReelNavigationGuard {
    private var lastObservedUrl: String? = null
    private var dmReelContextActive = false
    private var lastDmReelUrl: String? = null

    fun restoreFromHistory(urls: List<String>, currentIndex: Int) {
        if (currentIndex !in urls.indices || !isIndividualReelPath(pathOf(urls[currentIndex]))) return

        for (index in currentIndex - 1 downTo 0) {
            val previousPath = pathOf(urls[index]) ?: return
            if (isDirectPath(previousPath)) {
                dmReelContextActive = true
                lastDmReelUrl = urls[currentIndex]
                lastObservedUrl = urls[currentIndex]
                return
            }
            if (!isIndividualReelPath(previousPath)) return
        }
    }

    fun redirectTarget(nextUrl: String?): String? {
        val target = nextUrl ?: return null
        val targetUri = runCatching { URI(target) }.getOrNull()
        if (!isInstagramHost(targetUri?.host)) {
            dmReelContextActive = false
            lastDmReelUrl = null
            lastObservedUrl = target
            return null
        }
        val previousPath = pathOf(lastObservedUrl)
        val targetPath = targetUri.path?.replace(Regex("/+"), "/")?.trimEnd('/')

        if (isIndividualReelPath(targetPath)) {
            if (isDirectPath(previousPath)) dmReelContextActive = true
            if (dmReelContextActive) {
                lastDmReelUrl = target
            } else {
                lastObservedUrl = target
                return InstagramRoutes.DM_INBOX
            }
        } else if (isGenericReelsPath(targetPath) && dmReelContextActive) {
            return lastDmReelUrl ?: InstagramRoutes.DM_INBOX
        } else {
            dmReelContextActive = false
            lastDmReelUrl = null
        }

        lastObservedUrl = target
        return null
    }

    private fun pathOf(url: String?): String? =
        url?.let { runCatching { URI(it) }.getOrNull() }
            ?.takeIf { isInstagramHost(it.host) }
            ?.path
            ?.replace(Regex("/+"), "/")
            ?.trimEnd('/')

    private fun isInstagramHost(host: String?): Boolean =
        host.equals("instagram.com", ignoreCase = true) ||
            host.equals("www.instagram.com", ignoreCase = true)

    private fun isDirectPath(path: String?): Boolean =
        path == "/direct" || path?.startsWith("/direct/") == true

    private fun isIndividualReelPath(path: String?): Boolean =
        path == "/reel" || path?.startsWith("/reel/") == true

    private fun isGenericReelsPath(path: String?): Boolean =
        path == "/reels" || path?.startsWith("/reels/") == true
}
