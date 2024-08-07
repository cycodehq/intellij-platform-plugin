package com.cycode.plugin.utils

import io.sentry.Sentry
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

private fun getFileDigest(file: File): MessageDigest {
    val digest = MessageDigest.getInstance("SHA-256")
    val fileInputStream = FileInputStream(file)
    val buffer = ByteArray(8192)
    var bytesRead: Int

    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
    }

    fileInputStream.close()
    return digest
}

private fun getFileShaHash(file: File): String {
    val digest = getFileDigest(file)

    val sha256Hash = digest.digest()
    val shaBuilder = StringBuilder()
    for (b in sha256Hash) {
        shaBuilder.append(String.format("%02x", b))
    }

    return shaBuilder.toString()
}

fun verifyFileChecksum(file: File, sha256Checksum: String): Boolean {
    if (!file.exists()) {
        return false
    }

    try {
        return sha256Checksum.equals(getFileShaHash(file), ignoreCase = true)
    } catch (e: Exception) {
        Sentry.captureException(e)
        e.printStackTrace()
    }

    return false
}

fun verifyFileChecksum(file: String, sha256Checksum: String): Boolean {
    return verifyFileChecksum(File(file), sha256Checksum)
}

fun verifyDirContentChecksums(dir: String, checksumDb: Map<String, String>): Boolean {
    for ((filename, checksum) in checksumDb) {
        val filePath = File(dir, filename)
        if (!verifyFileChecksum(filePath, checksum)) {
            return false
        }
    }

    return true
}


fun parseOnedirChecksumDb(rawChecksumDb: String): Map<String, String> {
    val checksums = mutableMapOf<String, String>()
    val lines = rawChecksumDb.split("\n")
    for (line in lines) {
        val parts = line.split(" ")
        if (parts.size != 2) {
            continue
        }

        val (checksum, filename) = parts
        checksums[filename] = checksum
    }
    return checksums
}
