package com.cycode.plugin.managers

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliWrapper
import com.cycode.plugin.cli.models.AuthCheckResult
import com.cycode.plugin.cli.models.AuthResult
import com.cycode.plugin.cli.models.VersionResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.services.pluginState
import com.cycode.plugin.services.scanResults
import com.cycode.plugin.utils.verifyFileChecksum
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.io.File


enum class CliScanType {
    Secret, Sast, Sca, Iac
}


class CliManager(private val project: Project? = null) {
    private val githubReleaseManager = GitHubReleaseManager()
    private val downloadManager = DownloadManager()

    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()

    private val scanResults = scanResults()

    // TODO: get checksum from GitHub release info
    private val checksum = "6fbc3d107fc445f15aac2acfaf686bb5be880ce2b3962fd0ce336490b315c6c4"

    fun healthCheck(): Boolean {
        val cliVersionResult: CliResult<VersionResult> = CliWrapper(pluginSettings.cliPath).executeCommand("version")
        if (cliVersionResult is CliResult.Success) {
            pluginState.cliInstalled = true
            pluginState.cliVer = cliVersionResult.result.version
            return true
        }

        return false
    }

    fun checkAuth(): Boolean {
        val authCheckResult: CliResult<AuthCheckResult> =
            CliWrapper(pluginSettings.cliPath).executeCommand("auth", "check")
        if (authCheckResult is CliResult.Success) {
            pluginState.cliInstalled = true
            pluginState.cliAuthed = authCheckResult.result.result
            return pluginState.cliAuthed
        }

        return false
    }

    fun doAuth(): Boolean {
        val authResult: CliResult<AuthResult> = CliWrapper(pluginSettings.cliPath).executeCommand("auth")
        if (authResult is CliResult.Success) {
            pluginState.cliAuthed = authResult.result.result
            return pluginState.cliAuthed
        }

        return false
    }

    private inline fun <reified T> scanFile(filePath: String, scanType: CliScanType): CliResult<T> {
        val scanTypeString = scanType.name.toLowerCase()
        return CliWrapper(pluginSettings.cliPath)
            .executeCommand<T>("scan", "-t", scanTypeString, "path", filePath)
    }

    fun scanFileSecrets(filePath: String): CliResult<SecretScanResult> {
        val results = scanFile<SecretScanResult>(filePath, CliScanType.Secret)
        scanResults.secretsResults = results

        if (project != null) {
            // TODO(MarshalX): run only for the provided file?
            // rerun annotators
            DaemonCodeAnalyzer.getInstance(project).restart()
        }

        return results
    }

    fun shouldDownloadCli(localPath: String): Boolean {
        // return true if was downloaded

        if (pluginState.cliHash == null) {
            thisLogger().warn("Should download CLI because cliHash is Null")
            return true
        }

        if (!verifyFileChecksum(localPath, pluginState.cliHash!!)) {
            thisLogger().warn("Should download CLI because checksum is invalid")
            return true
        }

        // TODO(MarshalX): add support of offline mode.

        // TODO(MarshalX): add check on new version. Not on every start. Store lastCheckedAt date in persistent storage
        thisLogger().warn("CLI is downloaded and the checksum is valid. But maybe not the latest available version.")

        return false
    }

    fun downloadCli(owner: String, repo: String, localPath: String): File? {
        val releaseInfo = githubReleaseManager.getLatestReleaseInfo(owner, repo)

        if (releaseInfo != null) {
            val downloadedFile = downloadManager.downloadFile(
                releaseInfo.assets[0].browserDownloadUrl, checksum, localPath
            )
            downloadedFile?.setExecutable(true)

            pluginState.cliHash = checksum

            return downloadedFile
        }

        return null
    }
}
