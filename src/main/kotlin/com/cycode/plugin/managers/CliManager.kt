package com.cycode.plugin.managers

import java.io.File

class CliManager {
    private val githubReleaseManager = GitHubReleaseManager()
    private val downloadManager = DownloadManager()

    fun downloadLatestRelease(owner: String, repo: String, localPath: String): File? {
        val releaseInfo = githubReleaseManager.getLatestReleaseInfo(owner, repo)
        // TODO: get checksum from release info
        val checksum = "7c2259c01e0e617e6d39a88cfd6914433a00efabe0dbe0009c7c55dee9022a87"

        if (releaseInfo != null) {
            val downloadedFile = downloadManager.downloadFile(
                releaseInfo.assets[0].browser_download_url, checksum, localPath
            )
            downloadedFile?.setExecutable(true)

            return downloadedFile
        }

        return null
    }
}
