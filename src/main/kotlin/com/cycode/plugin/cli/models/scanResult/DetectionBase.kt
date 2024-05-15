package com.cycode.plugin.cli.models.scanResult

interface DetectionBase {
    val severity: String
    val detectionDetails: ScanDetectionDetailsBase

    fun getFormattedMessage(): String
    fun getFormattedNodeTitle(): String
}
