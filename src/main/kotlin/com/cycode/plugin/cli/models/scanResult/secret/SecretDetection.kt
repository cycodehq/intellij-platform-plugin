package com.cycode.plugin.cli.models.scanResult.secret

data class SecretDetection(
    val message: String,
    val detectionDetails: SecretDetectionDetails,
    val severity: String,
    val type: String,
    val detectionRuleId: String,  // TODO(MarshalX): actually UUID. annotate?
    val detectionTypeId: String,  // TODO(MarshalX): actually UUID. annotate?
)
