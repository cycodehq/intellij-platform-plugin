package com.cycode.plugin.cli.models.scanResult.secret

data class SecretDetection(
    val message: String,
    val detectionDetails: SecretDetectionDetails,
    val severity: String,
    val type: String,
    val detectionRuleId: String,  // UUID
    val detectionTypeId: String,  // UUID
)
