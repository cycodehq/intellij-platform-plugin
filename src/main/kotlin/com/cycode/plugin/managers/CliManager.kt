package com.cycode.plugin.managers

import java.io.File

class CliManager {
    private val githubReleaseManager = GitHubReleaseManager()
    private val downloadManager = DownloadManager()

    fun downloadLatestRelease(owner: String, repo: String, localPath: String): File? {
        val releaseInfo = githubReleaseManager.getLatestReleaseInfo(owner, repo)
        // TODO: get checksum from release info
        val checksum = "6fbc3d107fc445f15aac2acfaf686bb5be880ce2b3962fd0ce336490b315c6c4"

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
