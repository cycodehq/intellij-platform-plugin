package com.cycode.plugin.services

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.PROJECT)
class ScanResultsService {
    var secretsResults: CliResult<SecretScanResult>? = null

    init {
        thisLogger().info("CycodeResultsService init")
    }
}
