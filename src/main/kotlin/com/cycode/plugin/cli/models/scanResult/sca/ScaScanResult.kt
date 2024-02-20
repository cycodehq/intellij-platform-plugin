package com.cycode.plugin.cli.models.scanResult.sca

import com.cycode.plugin.cli.models.CliError
import com.cycode.plugin.cli.models.scanResult.ScanResultBase

data class ScaScanResult(
    override val detections: List<ScaDetection>,
    val errors: List<CliError>,
) : ScanResultBase
