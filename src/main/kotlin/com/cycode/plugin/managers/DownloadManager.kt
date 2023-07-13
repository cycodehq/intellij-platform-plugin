package com.cycode.plugin.managers

import com.intellij.openapi.diagnostic.thisLogger
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest


class DownloadManager {
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

            if (checksum == null || verifyChecksum(tempFile, checksum)) {
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

    private fun verifyChecksum(file: File, sha256Checksum: String): Boolean {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }

            fileInputStream.close()

            val sha256Hash = digest.digest()

            val shaBuilder = StringBuilder()
            for (b in sha256Hash) {
                shaBuilder.append(String.format("%02x", b))
            }

            val calculatedChecksum = shaBuilder.toString()

            thisLogger().warn("Excepted checksum: $sha256Checksum; actual checksum: $calculatedChecksum")

            return sha256Checksum.equals(calculatedChecksum, ignoreCase = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }
}
