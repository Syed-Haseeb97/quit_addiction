package com.example.dmsonly.web

import android.net.Uri

object InstagramRoutes {
    const val DM_INBOX = "https://www.instagram.com/direct/inbox/"
    private const val INSTAGRAM_HOST = "instagram.com"
    private const val WWW_INSTAGRAM_HOST = "www.instagram.com"

    val FEED_HOSTS = setOf(INSTAGRAM_HOST, WWW_INSTAGRAM_HOST)
    val BLOCKED_FIRST_SEGMENTS = setOf("explore", "reels")

    fun isInstagramHost(uri: Uri): Boolean = isInstagramHost(uri.host)
    fun isInstagramHost(host: String?): Boolean {
        val normalized = host?.lowercase() ?: return false
        return normalized == INSTAGRAM_HOST || normalized == WWW_INSTAGRAM_HOST
    }

    fun isInboxUrl(raw: String?): Boolean = runCatching {
        val uri = Uri.parse(raw ?: return false)
        isInstagramHost(uri) && normalizePath(uri.path) == "/direct/inbox"
    }.getOrDefault(false)

    fun isAuthFlowUrl(raw: String?): Boolean = runCatching {
        val uri = Uri.parse(raw ?: return false)
        isInstagramHost(uri) && isAuthenticationRoute(normalizePath(uri.path))
    }.getOrDefault(false)

    fun isBlocked(uri: Uri): Boolean = isInstagramHost(uri) && isBlockedPath(uri.path)
    fun isBlocked(raw: String?): Boolean = raw?.let { runCatching { isBlocked(Uri.parse(it)) }.getOrDefault(false) } == true

    fun decide(uri: Uri): Decision = when {
        isInstagramHost(uri) && isBlocked(uri) -> Decision.Redirect(DM_INBOX)
        isInstagramHost(uri) -> Decision.Allow
        uri.scheme == "http" || uri.scheme == "https" -> Decision.OpenExternally(uri)
        else -> Decision.Block
    }

    fun isBlockedPath(path: String?): Boolean {
        val normalized = normalizePath(path)
        if (normalized == "/") return true
        return normalized.removePrefix("/").substringBefore("/").lowercase() in BLOCKED_FIRST_SEGMENTS
    }

    private fun isAuthenticationRoute(path: String): Boolean =
        path == "/accounts/login" || path.startsWith("/accounts/login/") ||
        path == "/challenge" || path.startsWith("/challenge/") ||
        path == "/checkpoint" || path.startsWith("/checkpoint/") ||
        path == "/oauth" || path.startsWith("/oauth/") ||
        path == "/consent" || path.startsWith("/consent/")

    private fun normalizePath(path: String?): String {
        val value = path?.trim().orEmpty()
        if (value.isEmpty()) return "/"
        return value.replace(Regex("/+"), "/").let { if (it.length > 1) it.trimEnd('/') else it }
    }

    sealed interface Decision {
        data object Allow : Decision
        data object Block : Decision
        data class Redirect(val url: String) : Decision
        data class OpenExternally(val uri: Uri) : Decision
    }
}
