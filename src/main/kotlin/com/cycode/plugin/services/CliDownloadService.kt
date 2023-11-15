package com.cycode.plugin.services

import com.cycode.plugin.Consts
import com.cycode.plugin.utils.verifyFileChecksum
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.SystemInfo
import java.io.File


@Service(Service.Level.APP)
class CliDownloadService {
    private val githubReleaseService = githubReleases()
    private val downloadService = download()

    private val pluginState = pluginState()

    private var githubReleaseInfo: GitHubRelease? = null

    private fun getGitHubLatestRelease(dropCache: Boolean = false): GitHubRelease? {
        // prevent sending many requests
        if (githubReleaseInfo != null && !dropCache) {
            return githubReleaseInfo
        }

        githubReleaseInfo = githubReleaseService.getLatestReleaseInfo(Consts.CLI_GITHUB_ORG, Consts.CLI_GITHUB_REPO)
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

        val executableHashAsset = githubReleaseService.findAssetByFilename(releaseInfo.assets, executableAssetHashName)
        if (executableHashAsset == null) {
            thisLogger().warn("Failed to find executableHashAsset")
            return null
        }

        return downloadService.retrieveFileTextContent(executableHashAsset.browserDownloadUrl)
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

        val executableAsset = githubReleaseService.findAssetByFilename(releaseInfo.assets, executableAssetName)
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
            downloadService.downloadFile(executableAsset.browserDownloadUrl, expectedFileChecksum, localPath)
        downloadedFile?.setExecutable(true)

        pluginState.cliHash = expectedFileChecksum

        return downloadedFile
    }
}
