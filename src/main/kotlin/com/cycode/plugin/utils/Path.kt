package com.cycode.plugin.utils

import java.io.File
import java.io.IOException

fun isValidExistedFilePath(path: String): Boolean {
    val file = File(path)
    return try {
        file.canonicalPath
        file.exists()
    } catch (e: IOException) {
        false
    }
}
