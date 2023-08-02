package com.cycode.plugin.managers

import com.intellij.openapi.diagnostic.thisLogger
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class FileManager {
    fun verifyChecksum(file: File, sha256Checksum: String): Boolean {
        if (!file.exists()) {
            return false
        }

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

    fun verifyChecksum(file: String, sha256Checksum: String): Boolean {
        return verifyChecksum(File(file), sha256Checksum)
    }
}
