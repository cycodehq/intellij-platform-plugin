package com.cycode.plugin.managers

import com.cycode.plugin.services.pluginState
import com.cycode.plugin.utils.CliResult
import com.cycode.plugin.utils.CliWrapper
import com.intellij.openapi.diagnostic.thisLogger
import java.io.File

class CliManager {
    private val githubReleaseManager = GitHubReleaseManager()
    private val downloadManager = DownloadManager()
    private val fileManager = FileManager()

    private val pluginState = pluginState()

    // TODO: get checksum from GitHub release info
    private val checksum = "6fbc3d107fc445f15aac2acfaf686bb5be880ce2b3962fd0ce336490b315c6c4"

    fun healthCheck(): Boolean {
        val cliVersionResult = CliWrapper(pluginState.cliPath).executeCommand("version")
        if (cliVersionResult is CliResult.Success) {
            pluginState.cliVer = cliVersionResult.result["version"] as String
            return true
        }

        return false
    }

    fun checkAuth(): Boolean {
        val authCheckResult = CliWrapper(pluginState.cliPath).executeCommand("auth", "check")
        if (authCheckResult is CliResult.Success) {
            val autched = authCheckResult.result["result"] as Boolean
            pluginState.cliAuthed = autched
            return autched
        }

        return false
    }

    fun auth(): Boolean {
        val authResult = CliWrapper(pluginState.cliPath).executeCommand("auth")
        if (authResult is CliResult.Success) {
            val autched = authResult.result["result"] as Boolean
            pluginState.cliAuthed = autched
            return autched
        }

        return false
    }

    fun maybeDownloadCli(owner: String, repo: String, localPath: String): Boolean {
        // return true if was downloaded

        if (pluginState.cliHash == null) {
            thisLogger().warn("Downloading CLI because cliHash is Null")
            downloadLatestRelease(owner, repo, localPath)
            return true
        }

        if (!fileManager.verifyChecksum(localPath, pluginState.cliHash!!)) {
            thisLogger().warn("Downloading CLI because checksum is invalid")
            downloadLatestRelease(owner, repo, localPath)
            return true
        }

        // TODO(MarshalX): add support of offline mode.

        // TODO(MarshalX): add check on new version. Not on every start. Store lastCheckedAt date in persistent storage
        thisLogger().warn("CLI is downloaded and the checksum is valid. But maybe not the latest available version.")

        return false
    }

    private fun downloadLatestRelease(owner: String, repo: String, localPath: String): File? {
        val releaseInfo = githubReleaseManager.getLatestReleaseInfo(owner, repo)

        if (releaseInfo != null) {
            val downloadedFile = downloadManager.downloadFile(
                releaseInfo.assets[0].browser_download_url, checksum, localPath
            )
            downloadedFile?.setExecutable(true)

            pluginState.cliHash = checksum

            return downloadedFile
        }

        return null
    }
}
