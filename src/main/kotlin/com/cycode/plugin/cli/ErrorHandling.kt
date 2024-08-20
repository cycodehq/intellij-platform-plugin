package com.cycode.plugin.cli

import com.cycode.plugin.CycodeBundle

enum class ErrorCode {
    MISSING_C_STANDARD_LIBRARY,
    UNKNOWN
}

const val MISSING_C_STANDARD_LIBRARY_SEARCH = "GLIBC"

private fun caseInsensitiveSearch(output: String, search: String): Boolean {
    return output.lowercase().contains(search.lowercase())
}

fun detectErrorCode(output: String): ErrorCode {
    return when {
        caseInsensitiveSearch(output, MISSING_C_STANDARD_LIBRARY_SEARCH) -> ErrorCode.MISSING_C_STANDARD_LIBRARY
        else -> ErrorCode.UNKNOWN
    }
}

fun getUserFriendlyCliErrorMessage(errorCode: ErrorCode): String {
    return when (errorCode) {
        ErrorCode.MISSING_C_STANDARD_LIBRARY -> CycodeBundle.message("missingCStandardLibraryErrorNotification")
        else -> CycodeBundle.message("unknownErrorNotification")
    }
}
