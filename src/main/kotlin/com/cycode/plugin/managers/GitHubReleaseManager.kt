package com.cycode.plugin.managers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URL


data class GitHubReleaseAsset(
    val name: String,
    val browserDownloadUrl: String,
)


data class GitHubRelease(
    val tagName: String,
    val name: String,
    val assets: List<GitHubReleaseAsset>
)

class GitHubReleaseManager {
    var mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun getLatestReleaseInfo(owner: String, repo: String): GitHubRelease? {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases"

        try {
            val url = URL(apiUrl)
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            val inputStream = connection.getInputStream()
            val response = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            val releases: List<GitHubRelease> = mapper.readValue(response)

            if (releases.isNotEmpty()) {
                return releases[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
