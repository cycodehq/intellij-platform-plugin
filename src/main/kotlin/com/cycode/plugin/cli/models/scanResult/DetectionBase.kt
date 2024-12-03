package com.cycode.plugin.cli.models.scanResult

interface DetectionBase {
    val id: String
    val severity: String
    val detectionDetails: ScanDetectionDetailsBase

    fun getFormattedMessage(): String
    fun getFormattedNodeTitle(): String
}
