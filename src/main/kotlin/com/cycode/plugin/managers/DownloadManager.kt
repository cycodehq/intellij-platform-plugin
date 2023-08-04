package com.cycode.plugin.managers

import com.cycode.plugin.utils.verifyFileChecksum
import com.intellij.openapi.diagnostic.thisLogger
import java.io.*
import java.net.URL
import java.nio.file.Files


class DownloadManager {
    private fun shouldSaveFile(tempFile: File, checksum: String?): Boolean {
        // if we don't expect checksum validation or checksum is valid
        return checksum == null || verifyFileChecksum(tempFile, checksum)
    }

    fun downloadFile(url: String, checksum: String?, localPath: String): File? {
        thisLogger().warn("Downloading $url with checksum $checksum")
        thisLogger().warn("Expecting to download to $localPath")

        val file = File(localPath)
        val tempFile = File.createTempFile("cycode-", ".tmp")

        thisLogger().warn("Temp path: ${tempFile.absolutePath}")

        try {
            val urlObj = URL(url)
            val connection = urlObj.openConnection()
            connection.connect()

            val inputStream = BufferedInputStream(connection.getInputStream())
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()

            if (shouldSaveFile(tempFile, checksum)) {
                if (file.exists()) {
                    file.delete()
                }

                // TODO(MarshalX): use atomic move; fallback to tempFile.renameTo(file) on error?
                Files.move(
                    tempFile.toPath(),
                    file.toPath()
                )

                return file
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to download file $e")
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }

        return null
    }
}
