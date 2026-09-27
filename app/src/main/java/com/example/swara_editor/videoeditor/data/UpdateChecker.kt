package com.example.swara_editor.videoeditor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String = "v0.0.1",
    val latestVersion: String,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

object UpdateChecker {

    const val CURRENT_VERSION = "v0.0.1"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/kannan-ai/swara_editor/releases/latest"

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "SwaraEditor-AndroidApp")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)
                val tagName = json.optString("tag_name", CURRENT_VERSION)
                val releaseTitle = json.optString("name", "Swara Editor $tagName")
                val releaseNotes = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/kannan-ai/swara_editor/releases")

                val isNewer = isVersionNewer(CURRENT_VERSION, tagName)

                UpdateInfo(
                    hasUpdate = isNewer,
                    currentVersion = CURRENT_VERSION,
                    latestVersion = tagName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = htmlUrl
                )
            } else {
                UpdateInfo(
                    hasUpdate = false,
                    currentVersion = CURRENT_VERSION,
                    latestVersion = CURRENT_VERSION,
                    releaseTitle = "Swara Editor $CURRENT_VERSION",
                    releaseNotes = "You are running the latest version.",
                    downloadUrl = "https://github.com/kannan-ai/swara_editor"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(
                hasUpdate = false,
                currentVersion = CURRENT_VERSION,
                latestVersion = CURRENT_VERSION,
                releaseTitle = "Swara Editor $CURRENT_VERSION",
                releaseNotes = "Unable to connect to GitHub. Check internet connection.",
                downloadUrl = "https://github.com/kannan-ai/swara_editor"
            )
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currClean = current.removePrefix("v").trim()
        val lateClean = latest.removePrefix("v").trim()
        return lateClean != currClean && lateClean > currClean
    }
}
