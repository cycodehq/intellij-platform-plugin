package com.cycode.plugin.services

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.components.Service
import io.sentry.Sentry
import java.net.URI


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
    private var mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun getJson(url: String): String? {
        try {
            val urlObj = URI(url).toURL()
            val connection = urlObj.openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            val inputStream = connection.getInputStream()
            val response = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            return response
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun getReleaseInfoByTag(owner: String, repo: String, tag: String): GitHubRelease? {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/tags/$tag"

        return try {
            val response = getJson(apiUrl) ?: return null
            val release: GitHubRelease = mapper.readValue(response)
            release
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getLatestReleaseInfo(owner: String, repo: String): GitHubRelease? {
        // TODO(MarshalX): probably we should not download major releases.
        //  we should store the current supported major release (for example 1) in the plugin
        //  and download only minor releases (1.1, 1.2, etc.)
        //  it will be easier to manage breaking changes in CLI

        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases"

        return try {
            val response = getJson(apiUrl) ?: return null

            val releases: List<GitHubRelease> = mapper.readValue(response)
            if (releases.isNotEmpty()) {
                releases[0]
            }

            null
        } catch (e: Exception) {
            Sentry.captureException(e)
            e.printStackTrace()
            null
        }
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
