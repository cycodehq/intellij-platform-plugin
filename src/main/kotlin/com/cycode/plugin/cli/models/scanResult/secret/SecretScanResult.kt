package com.cycode.plugin.cli.models.scanResult.secret

import com.cycode.plugin.cli.models.CliError

data class SecretScanResult(
    val detections: List<SecretDetection>,
    val errors: List<CliError>,
)
