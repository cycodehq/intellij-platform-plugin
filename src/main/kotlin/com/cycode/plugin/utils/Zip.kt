package com.cycode.plugin.utils

import java.io.File
import java.util.zip.ZipFile


fun unzip(zipFile: File, destDir: String) {
    ZipFile(zipFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            zip.getInputStream(entry).use { input ->
                val destFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile.mkdirs()
                    input.copyTo(destFile.outputStream())
                }
            }
        }
    }
}
