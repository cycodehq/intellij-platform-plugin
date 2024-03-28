package com.cycode.plugin.cli.models.scanResult.iac

import com.cycode.plugin.cli.models.CliError
import com.cycode.plugin.cli.models.scanResult.ScanResultBase

data class IacScanResult(
    override val detections: List<IacDetection>,
    override val errors: List<CliError>,
) : ScanResultBase
