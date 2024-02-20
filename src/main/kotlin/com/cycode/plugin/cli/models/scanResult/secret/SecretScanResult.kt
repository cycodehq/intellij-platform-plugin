package com.cycode.plugin.cli.models.scanResult.secret

import com.cycode.plugin.cli.models.CliError
import com.cycode.plugin.cli.models.scanResult.ScanResultBase

data class SecretScanResult(
    override val detections: List<SecretDetection>,
    val errors: List<CliError>,
) : ScanResultBase
