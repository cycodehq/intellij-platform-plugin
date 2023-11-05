package com.cycode.plugin.managers

import com.cycode.plugin.Consts
import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliWrapper
import com.cycode.plugin.cli.ExitCodes
import com.cycode.plugin.cli.models.AuthCheckResult
import com.cycode.plugin.cli.models.AuthResult
import com.cycode.plugin.cli.models.VersionResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.services.pluginState
import com.cycode.plugin.services.scanResults
import com.cycode.plugin.utils.CycodeNotifier
import com.cycode.plugin.utils.verifyFileChecksum
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File


enum class CliScanType {
    Secret, Sast, Sca, Iac
}


class CliManager(private val project: Project) {
    private val githubReleaseManager = GitHubReleaseManager()
    private val downloadManager = DownloadManager()

    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()

    private val scanResults = scanResults()

    private var githubReleaseInfo: GitHubRelease? = null

    private val cli: CliWrapper

    var cliShouldDestroyCallback: (() -> Boolean)? = null

    init {
        cli = CliWrapper(pluginSettings.cliPath, getWorkingDirectory())
    }

    private fun resetPluginCLiState() {
        pluginState.cliAuthed = false
        pluginState.cliInstalled = false
        pluginState.cliVer = null
    }

    private fun showErrorNotification(message: String) {
        CycodeNotifier.notifyError(project, message)
    }

    private fun <T> processResult(result: CliResult<T>): CliResult<T>? {
        if (result is CliResult.Error) {
            showErrorNotification(result.result.message)
            return null
        }
        if (result is CliResult.Panic) {
            if (result.exitCode == ExitCodes.TERMINATION) {
                // don't notify user about user-requested terminations
                return null
            }

            showErrorNotification(result.errorMessage)
            return null
        }

        return result
    }

    private fun getGitHubLatestRelease(dropCache: Boolean = false): GitHubRelease? {
        // prevent sending many requests
        if (githubReleaseInfo != null && !dropCache) {
            return githubReleaseInfo
        }

        githubReleaseInfo = githubReleaseManager.getLatestReleaseInfo(Consts.CLI_GITHUB_ORG, Consts.CLI_GITHUB_REPO)
        return githubReleaseInfo
    }

    private fun getOperatingSystemRelatedCliFilename(): String? {
        return when {
            SystemInfo.isWindows -> "cycode-win.exe"
            SystemInfo.isMac -> "cycode-mac"
            SystemInfo.isLinux -> "cycode-linux"
            else -> null
        }
    }

    private fun getOperatingSystemRelatedCliHashFilename(): String? {
        val filename = getOperatingSystemRelatedCliFilename() ?: return null
        return "$filename.sha256"
    }

    private fun getWorkingDirectory(): String? {
        val modules = ModuleManager.getInstance(project).modules
        if (modules.isEmpty()) {
            return null
        }

        val module = modules[0]
        return module.project.basePath
    }

    fun healthCheck(): Boolean {
        val result: CliResult<VersionResult> =
            CliWrapper(pluginSettings.cliPath, getWorkingDirectory()).executeCommand(
                "version",
                shouldDestroyCallback = cliShouldDestroyCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginState.cliInstalled = true
            pluginState.cliVer = processedResult.result.version
            return true
        }

        resetPluginCLiState()
        return false
    }

    fun checkAuth(): Boolean {
        val result: CliResult<AuthCheckResult> =
            CliWrapper(pluginSettings.cliPath, getWorkingDirectory()).executeCommand(
                "auth",
                "check",
                shouldDestroyCallback = cliShouldDestroyCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginState.cliInstalled = true
            pluginState.cliAuthed = processedResult.result.result
            if (!pluginState.cliAuthed) {
                showErrorNotification(CycodeBundle.message("checkAuthErrorNotification"))
            }

            return pluginState.cliAuthed
        }

        resetPluginCLiState()
        return false
    }

    fun doAuth(): Boolean {
        val result: CliResult<AuthResult> =
            CliWrapper(pluginSettings.cliPath, getWorkingDirectory()).executeCommand(
                "auth",
                shouldDestroyCallback = cliShouldDestroyCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginState.cliAuthed = processedResult.result.result
            if (!pluginState.cliAuthed) {
                showErrorNotification(CycodeBundle.message("authErrorNotification"))
            }
            return pluginState.cliAuthed
        }

        return false
    }

    fun ignore(optionName: String, optionValue: String): Boolean {
        val result: CliResult<Unit> = CliWrapper(pluginSettings.cliPath, getWorkingDirectory()).executeCommand(
            "ignore",
            optionName,
            optionValue,
            shouldDestroyCallback = cliShouldDestroyCallback
        )

        val processedResult = processResult(result)
        return processedResult is CliResult.Success
    }

    private inline fun <reified T> scanFile(filePath: String, scanType: CliScanType): CliResult<T>? {
        val scanTypeString = scanType.name.toLowerCase()
        val result = CliWrapper(pluginSettings.cliPath, getWorkingDirectory())
            .executeCommand<T>(
                "scan",
                "-t",
                scanTypeString,
                "path",
                filePath,
                shouldDestroyCallback = cliShouldDestroyCallback
            )

        return processResult(result)
    }

    fun scanFileSecrets(filePath: String) {
        val results = scanFile<SecretScanResult>(filePath, CliScanType.Secret)
        if (results == null) {
            thisLogger().warn("Failed to scan file: $filePath")
            return
        }
        scanResults.secretsResults = results

        // TODO(MarshalX): run only for the provided file?
        // rerun annotators
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    private fun shouldDownloadNewRemoteCli(localPath: String): Boolean {
        val timeNow = System.currentTimeMillis()

        if (pluginState.cliLastUpdateCheckedAt == null) {
            pluginState.cliLastUpdateCheckedAt = timeNow
            thisLogger().warn("Should not download CLI because cliLastUpdateCheckedAt is Null. First plugin run.")
            return false
        }

        val diffInSec = (timeNow - pluginState.cliLastUpdateCheckedAt!!) / 1000
        if (diffInSec < Consts.CLI_CHECK_NEW_VERSION_EVERY_SEC) {
            thisLogger().warn(
                "Should not check remote CLI version because diffInSec is $diffInSec " +
                        "(less than ${Consts.CLI_CHECK_NEW_VERSION_EVERY_SEC})"
            )
            return false
        }

        val remoteChecksum = getRemoteChecksum(true)
        if (remoteChecksum == null) {
            thisLogger().warn(
                "Should not download new CLI because can't get remoteChecksum. " +
                        "Maybe no internet connection."
            )
            return false
        }

        if (!verifyFileChecksum(localPath, remoteChecksum)) {
            thisLogger().warn("Should download CLI because checksum doesn't mach remote checksum ($remoteChecksum)")
            return true
        }

        return false
    }

    fun shouldDownloadCli(localPath: String): Boolean {
        if (pluginState.cliHash == null) {
            thisLogger().warn("Should download CLI because cliHash is Null")
            return true
        }

        if (!verifyFileChecksum(localPath, pluginState.cliHash!!)) {
            thisLogger().warn("Should download CLI because checksum is invalid")
            return true
        }

        if (shouldDownloadNewRemoteCli(localPath)) {
            return true
        }

        thisLogger().warn("CLI is downloaded and the checksum is valid.")
        return false
    }

    private fun getRemoteChecksum(dropCache: Boolean = false): String? {
        val releaseInfo = getGitHubLatestRelease(dropCache)
        if (releaseInfo == null) {
            thisLogger().warn("Failed to get latest release info")
            return null
        }

        val executableAssetHashName = getOperatingSystemRelatedCliHashFilename()
        if (executableAssetHashName == null) {
            thisLogger().warn("Failed to get asset names. Unknown operating system")
            return null
        }

        val executableHashAsset = githubReleaseManager.findAssetByFilename(releaseInfo.assets, executableAssetHashName)
        if (executableHashAsset == null) {
            thisLogger().warn("Failed to find executableHashAsset")
            return null
        }

        return downloadManager.retrieveFileTextContent(executableHashAsset.browserDownloadUrl)
    }

    private fun getExecutableAsset(): GitHubReleaseAsset? {
        val releaseInfo = getGitHubLatestRelease()
        if (releaseInfo == null) {
            thisLogger().warn("Failed to get latest release info")
            return null
        }

        val executableAssetName = getOperatingSystemRelatedCliFilename()
        if (executableAssetName == null) {
            thisLogger().warn("Failed to get asset names. Unknown operating system")
            return null
        }

        val executableAsset = githubReleaseManager.findAssetByFilename(releaseInfo.assets, executableAssetName)
        if (executableAsset == null) {
            thisLogger().warn("Failed to find executableAsset")
            return null
        }

        return executableAsset
    }

    fun downloadCli(localPath: String): File? {
        val executableAsset = getExecutableAsset()
        if (executableAsset == null) {
            thisLogger().warn("Failed to get executableAsset")
            return null
        }

        val expectedFileChecksum = getRemoteChecksum()
        if (expectedFileChecksum == null) {
            thisLogger().warn("Failed to get expectedFileChecksum")
            return null
        }

        val downloadedFile =
            downloadManager.downloadFile(executableAsset.browserDownloadUrl, expectedFileChecksum, localPath)
        downloadedFile?.setExecutable(true)

        pluginState.cliHash = expectedFileChecksum

        return downloadedFile
    }
}
