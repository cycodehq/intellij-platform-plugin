package com.cycode.plugin.cli.models.scanResult

import com.cycode.plugin.cli.models.CliError

interface ScanResultBase {
    val detections: List<DetectionBase>
    val errors: List<CliError>
}
