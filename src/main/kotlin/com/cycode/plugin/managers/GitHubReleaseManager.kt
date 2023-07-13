package com.cycode.plugin.managers

import com.google.gson.Gson
import java.net.URL


data class GitHubReleaseAsset(
    val name: String,
    val browser_download_url: String,
)


data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val downloadUrl: String,
    val assets: Array<GitHubReleaseAsset>
)

class GitHubReleaseManager {
    fun getLatestReleaseInfo(owner: String, repo: String): GitHubRelease? {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases"

        try {
            val url = URL(apiUrl)
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            val inputStream = connection.getInputStream()
            val response = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            val releases = Gson().fromJson(response, Array<GitHubRelease>::class.java)

            if (releases.isNotEmpty()) {
                return releases[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
