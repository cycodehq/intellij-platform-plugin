package com.cycode.plugin.services

import com.cycode.plugin.Consts
import com.cycode.plugin.utils.parseOnedirChecksumDb
import com.cycode.plugin.utils.unzip
import com.cycode.plugin.utils.verifyDirContentChecksums
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
    private val pluginSettings = pluginSettings()

    private var githubReleaseInfo: GitHubRelease? = null

    // shared lock for the entire application (all projects)
    // used with calling of initCli() method
    val initCliLock = Any()

    fun initCli() {
        // if the CLI path is not overriden and executable is auto managed, and need to download - download it.
        if (
            pluginSettings.cliPath == Consts.DEFAULT_CLI_PATH &&
            pluginSettings.cliAutoManaged &&
            shouldDownloadCli()
        ) {
            downloadCli()
            thisLogger().info("CLI was successfully downloaded/updated")
        }
    }

    private fun getGitHubSupportedRelease(forceRefresh: Boolean = false): GitHubRelease? {
        // prevent sending many requests
        if (githubReleaseInfo != null && !forceRefresh) {
            return githubReleaseInfo
        }

        githubReleaseInfo = githubReleaseService.getReleaseInfoByTag(
            Consts.CLI_GITHUB_ORG,
            Consts.CLI_GITHUB_REPO,
            Consts.CLI_GITHUB_TAG
        )
        return githubReleaseInfo
    }

    private fun getGitHubLatestRelease(forceRefresh: Boolean = false): GitHubRelease? {
        // not used because could break old versions with breaking changes in CLI

        // prevent sending many requests
        if (githubReleaseInfo != null && !forceRefresh) {
            return githubReleaseInfo
        }

        githubReleaseInfo = githubReleaseService.getLatestReleaseInfo(Consts.CLI_GITHUB_ORG, Consts.CLI_GITHUB_REPO)
        return githubReleaseInfo
    }

    private fun getOperatingSystemRelatedReleaseAssetFilename(): String? {
        val isAarch64 = SystemInfo.OS_ARCH == "aarch64"
        return when {
            SystemInfo.isWindows -> "cycode-win.exe"
            SystemInfo.isMac && isAarch64 -> "cycode-mac-arm-onedir.zip"
            SystemInfo.isMac && !isAarch64 -> "cycode-mac-onedir.zip"
            SystemInfo.isLinux -> "cycode-linux"
            else -> null
        }
    }

    private fun getOperatingSystemRelatedReleaseAssetHashFilename(): String? {
        val filename = getOperatingSystemRelatedReleaseAssetFilename() ?: return null

        // TODO(MarshalX): mb we should rename GitHub asset to remove this hack
        //   but there is question about .sha256 of zip and zip content
        if (filename.endsWith(".zip")) {
            return filename.substring(0, filename.length - 4) + ".sha256"
        }

        return "$filename.sha256"
    }

    private fun shouldDownloadNewRemoteCli(localPath: String, isDir: Boolean): Boolean {
        if (pluginState.cliVer != Consts.REQUIRED_CLI_VERSION) {
            thisLogger().warn("Should download CLI because version missmatch")
            return true
        }

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
        } else {
            pluginState.cliLastUpdateCheckedAt = timeNow
        }

        val remoteChecksum = getRemoteChecksumFile(true)
        if (remoteChecksum == null) {
            thisLogger().warn(
                "Should not download new CLI because can't get remoteChecksum. " +
                        "Maybe no internet connection."
            )
            return false
        }

        val isValidChecksum = if (isDir) {
            verifyDirContentChecksums(localPath, parseOnedirChecksumDb(remoteChecksum))
        } else {
            verifyFileChecksum(localPath, remoteChecksum)
        }

        if (!isValidChecksum) {
            thisLogger().warn("Should download CLI because checksum doesn't mach remote checksum")
            return true
        }

        return false
    }

    private fun shouldDownloadCli(): Boolean {
        if (SystemInfo.isMac) {
            return shouldDownloadOnedirCli()
        }

        return shouldDownloadSingleCliExecutable()
    }

    private fun shouldDownloadSingleCliExecutable(): Boolean {
        if (pluginState.cliHash == null) {
            thisLogger().warn("Should download CLI because cliHash is Null")
            return true
        }

        if (!verifyFileChecksum(Consts.DEFAULT_CLI_PATH, pluginState.cliHash!!)) {
            thisLogger().warn("Should download CLI because checksum is invalid")
            return true
        }

        if (shouldDownloadNewRemoteCli(Consts.DEFAULT_CLI_PATH, isDir=false)) {
            return true
        }

        thisLogger().warn("CLI is downloaded and the checksum is valid.")
        return false
    }

    private fun shouldDownloadOnedirCli(): Boolean {
        if (pluginState.cliDirHashes == null) {
            thisLogger().warn("Should download CLI because cliDirHashes is Null")
            return true
        }

        if (!verifyDirContentChecksums(Consts.PLUGIN_PATH, pluginState.cliDirHashes!!)) {
            thisLogger().warn("Should download CLI because one of checksum is invalid")
            return true
        }

        if (shouldDownloadNewRemoteCli(Consts.PLUGIN_PATH, isDir=true)) {
            return true
        }

        thisLogger().warn("CLI is downloaded and the checksums are valid.")
        return false
    }

    private fun getRemoteChecksumFile(forceRefresh: Boolean = false): String? {
        val releaseInfo = getGitHubSupportedRelease(forceRefresh)
        if (releaseInfo == null) {
            thisLogger().warn("Failed to get latest release info")
            return null
        }

        val executableAssetHashName = getOperatingSystemRelatedReleaseAssetHashFilename()
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
        val releaseInfo = getGitHubSupportedRelease()
        if (releaseInfo == null) {
            thisLogger().warn("Failed to get latest release info")
            return null
        }

        val executableAssetName = getOperatingSystemRelatedReleaseAssetFilename()
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

    private fun getAssetAndFileChecksum(): Pair<GitHubReleaseAsset, String>? {
        val executableAsset = getExecutableAsset()
        if (executableAsset == null) {
            thisLogger().warn("Failed to get executableAsset")
            return null
        }

        val expectedFileChecksum = getRemoteChecksumFile()
        if (expectedFileChecksum == null) {
            thisLogger().warn("Failed to get expectedFileChecksum")
            return null
        }

        return Pair(executableAsset, expectedFileChecksum)
    }

    private fun downloadCli(): File? {
        if (SystemInfo.isMac) {
            return downloadOnedirCli()
        }

        return downloadSingleCliExecutable()
    }

    private fun downloadSingleCliExecutable(): File? {
        val assetAndFileChecksum = getAssetAndFileChecksum()
        if (assetAndFileChecksum == null) {
            thisLogger().warn("Failed to get assetAndFileChecksum")
            return null
        }
        val (executableAsset, expectedFileChecksum) = assetAndFileChecksum

        val downloadedFile = downloadService.downloadFile(
            executableAsset.browserDownloadUrl,
            expectedFileChecksum,
            Consts.DEFAULT_CLI_PATH
        )
        downloadedFile?.setExecutable(true)

        pluginState.cliHash = expectedFileChecksum

        return downloadedFile
    }

    private fun downloadOnedirCli(): File? {
        val assetAndFileChecksum = getAssetAndFileChecksum()
        if (assetAndFileChecksum == null) {
            thisLogger().warn("Failed to get assetAndFileChecksum")
            return null
        }
        val (executableAsset, expectedDirContentChecksums) = assetAndFileChecksum

        val pathToZip = File(Consts.PLUGIN_PATH, "cycode-cli.zip")
        // we don't verify the checksum of the directory because it's not a single file
        // we will verify the checksum of the files inside the directory later
        downloadService.downloadFile(executableAsset.browserDownloadUrl, null, pathToZip)

        val cliExecutableFile = File(Consts.DEFAULT_CLI_PATH)
        val pathToCliDir = cliExecutableFile.parent
        unzip(pathToZip, pathToCliDir)
        pathToZip.delete()

        cliExecutableFile.setExecutable(true)

        // migrate old macOS users to onedir mode
        pluginState.cliHash = null

        pluginState.cliDirHashes = parseOnedirChecksumDb(expectedDirContentChecksums)

        return cliExecutableFile
    }
}
