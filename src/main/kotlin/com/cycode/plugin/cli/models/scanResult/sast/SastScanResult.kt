package com.cycode.plugin.cli.models.scanResult.sast

import com.cycode.plugin.cli.models.CliError
import com.cycode.plugin.cli.models.scanResult.ScanResultBase

data class SastScanResult(
    override val detections: List<SastDetection>,
    override val errors: List<CliError>,
) : ScanResultBase
