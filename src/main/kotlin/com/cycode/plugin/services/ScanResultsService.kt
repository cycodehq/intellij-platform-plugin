package com.cycode.plugin.services

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange

@Service(Service.Level.PROJECT)
class ScanResultsService {
    private val detectedSegments = mutableMapOf<Pair<CliScanType, TextRange>, String>()
    private var secretsResults: CliResult<SecretScanResult>? = null

    init {
        thisLogger().info("CycodeResultsService init")
    }

    fun setSecretsResults(result: CliResult<SecretScanResult>) {
        clearDetectedSegments()
        secretsResults = result
    }

    fun getSecretsResults(): CliResult<SecretScanResult>? {
        return secretsResults
    }

    fun clear() {
        secretsResults = null
    }

    fun hasResults(): Boolean {
        return secretsResults != null
    }

    fun saveDetectedSegment(scanType: CliScanType, textRange: TextRange, value: String) {
        detectedSegments[Pair(scanType, textRange)] = value
    }

    fun getDetectedSegment(scanType: CliScanType, textRange: TextRange): String? {
        return detectedSegments[Pair(scanType, textRange)]
    }

    private fun clearDetectedSegments() {
        detectedSegments.clear()
    }
}
