package com.cycode.plugin.services

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.components.Service
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

@Service(Service.Level.APP)
class GithubReleaseService {
    var mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun getLatestReleaseInfo(owner: String, repo: String): GitHubRelease? {
        // TODO(MarshalX): probably we should not download major releases.
        //  we should store the current supported major release (for example 1) in the plugin
        //  and download only minor releases (1.1, 1.2, etc.)
        //  it will be easier to manage breaking changes in CLI

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

    fun findAssetByFilename(assets: List<GitHubReleaseAsset>, filename: String): GitHubReleaseAsset? {
        for (asset in assets) {
            if (asset.name == filename) {
                return asset
            }
        }

        return null
    }
}
