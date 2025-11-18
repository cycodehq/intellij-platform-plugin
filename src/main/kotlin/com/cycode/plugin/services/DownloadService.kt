package com.cycode.plugin.services

import com.cycode.plugin.utils.verifyFileChecksum
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import io.sentry.Sentry
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files


@Service(Service.Level.APP)
class DownloadService {
    private fun shouldSaveFile(tempFile: File, checksum: String?): Boolean {
        // if we don't expect checksum validation or checksum is valid
        return checksum == null || verifyFileChecksum(tempFile, checksum)
    }

    fun retrieveFileTextContent(url: String): String? {
        thisLogger().warn("Retrieving $url")

        try {
            val urlObj = URI(url).toURL()
            val connection = urlObj.openConnection()
            connection.connect()

            val inputStream = BufferedInputStream(connection.getInputStream())
            val content = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            return content
        } catch (e: Exception) {
            Sentry.captureException(e)
            thisLogger().error("Failed to download file $e", e)
        }

        return null
    }

    fun downloadFile(url: String, checksum: String?, localPath: File): File? {
        return downloadFile(url, checksum, localPath.toString())
    }

    fun downloadFile(url: String, checksum: String?, localPath: String): File? {
        thisLogger().warn("Downloading $url with checksum $checksum")
        thisLogger().warn("Expecting to download to $localPath")

        val file = File(localPath)
        val tempFile = File.createTempFile("cycode-", ".tmp")

        thisLogger().warn("Temp path: ${tempFile.absolutePath}")

        try {
            val urlObj = URI(url).toURL()
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

                try {
                    Files.createDirectories(file.toPath().parent)
                } catch (e: Exception) {
                    thisLogger().info("Failed to create directories for $file. Probably exists already", e)
                }

                // TODO(MarshalX): use atomic move; fallback to tempFile.renameTo(file) on error?
                Files.move(
                    tempFile.toPath(),
                    file.toPath()
                )

                return file
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            thisLogger().error("Failed to download file $e", e)
        } finally {
            tempFile.delete()
        }

        return null
    }
}
