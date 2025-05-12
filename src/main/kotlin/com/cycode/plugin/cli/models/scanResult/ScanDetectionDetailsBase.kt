package com.cycode.plugin.cli.models.scanResult

interface ScanDetectionDetailsBase {
    fun getFilepath(): String
    /**
     * Gets the line number.
     * @return The 1-based line number (first line is 1, not 0)
     */
    fun getLineNumber(): Int
}
