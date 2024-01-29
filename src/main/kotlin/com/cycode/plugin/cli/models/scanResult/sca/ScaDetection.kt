package com.cycode.plugin.cli.models.scanResult.sca

data class ScaDetection(
    val message: String,
    val detectionDetails: ScaDetectionDetails,
    val severity: String,
    val type: String,
    val detectionRuleId: String,
    val detectionTypeId: String,
)
