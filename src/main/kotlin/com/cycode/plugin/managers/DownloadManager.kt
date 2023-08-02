package com.cycode.plugin.managers

import com.intellij.openapi.diagnostic.thisLogger
import java.io.*
import java.net.URL
import java.nio.file.Files


class DownloadManager {
    private val fileManager = FileManager()

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

            if (checksum == null || fileManager.verifyChecksum(tempFile, checksum)) {
                if (file.exists()) {
                    file.delete()
                }

                // TODO: use atomic move, handle exceptions
                Files.move(
                    tempFile.toPath(),
                    file.toPath()
                )
//                val renameResult = tempFile.renameTo(file);
//                if (!renameResult) {
//                    thisLogger().error("Failed to rename file")
//                }

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
