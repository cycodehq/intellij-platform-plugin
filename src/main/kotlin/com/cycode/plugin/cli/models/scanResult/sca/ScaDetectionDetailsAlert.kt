package com.cycode.plugin.cli.models.scanResult.sca

data class ScaDetectionDetailsAlert(
    val severity: String,
    val summary: String,
    val description: String,
    val vulnerableRequirements: String?,
    val firstPatchedVersion: String?,
)
