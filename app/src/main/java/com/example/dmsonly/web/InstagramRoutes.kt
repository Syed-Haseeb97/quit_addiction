package com.example.dmsonly.web

import android.net.Uri

object InstagramRoutes {
    const val DM_INBOX = "https://www.instagram.com/direct/inbox/"
    private const val INSTAGRAM_HOST = "instagram.com"
    private const val WWW_INSTAGRAM_HOST = "www.instagram.com"

    fun isInstagramHost(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return host == INSTAGRAM_HOST || host == WWW_INSTAGRAM_HOST
    }

    fun isBlocked(uri: Uri): Boolean {
        if (!isInstagramHost(uri)) return false

        val path = normalizePath(uri.path)

        if (isAuthenticationRoute(path)) return false

        return path == "/" ||
            path == "/explore" ||
            path.startsWith("/explore/") ||
            path == "/reels" ||
            path.startsWith("/reels/")
    }

    private fun isAuthenticationRoute(path: String): Boolean {
        return path == "/accounts/login" ||
            path.startsWith("/accounts/login/") ||
            path == "/challenge" ||
            path.startsWith("/challenge/") ||
            path == "/checkpoint" ||
            path.startsWith("/checkpoint/") ||
            path == "/oauth" ||
            path.startsWith("/oauth/") ||
            path == "/consent" ||
            path.startsWith("/consent/")
    }

    private fun normalizePath(path: String?): String {
        val value = path?.trim().orEmpty()
        if (value.isEmpty()) return "/"
        return if (value.endsWith("/") && value.length > 1) value.dropLast(1) else value
    }
}
