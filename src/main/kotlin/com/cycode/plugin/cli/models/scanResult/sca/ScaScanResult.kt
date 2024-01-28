package com.cycode.plugin.cli.models.scanResult.sca

import com.cycode.plugin.cli.models.CliError

data class ScaScanResult(
    val detections: List<ScaDetection>,
    val errors: List<CliError>,
)
