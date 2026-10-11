package com.example.dmsonly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class AvailableUpdate(val version: String, val releaseUrl: String)

object UpdateChecker {
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/Syed-Haseeb97/quit_addiction/releases/latest"

    suspend fun check(currentVersion: String): AvailableUpdate? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val tag = Regex("\\"tag_name\\"\\s*:\\s*\\"([^\\"]+)\\"")
                .find(json)?.groupValues?.getOrNull(1) ?: return@withContext null
            val url = Regex("\\"html_url\\"\\s*:\\s*\\"([^\\"]+)\\"")
                .find(json)?.groupValues?.getOrNull(1) ?: return@withContext null
            val latest = tag.removePrefix("v")
            val current = currentVersion.removePrefix("v")
            if (compareVersions(latest, current) > 0) AvailableUpdate(latest, url) else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    internal fun compareVersions(left: String, right: String): Int {
        val a = left.split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val difference = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }
}
