package com.cycode.plugin.services

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.sca.ScaScanResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.services.scanResultsFilters.SecretScanResultsFilter
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange

@Service(Service.Level.PROJECT)
class ScanResultsService {
    private val detectedSegments = mutableMapOf<Pair<CliScanType, TextRange>, String>()
    private var secretResults: CliResult<SecretScanResult>? = null
    private var scaResults: CliResult<ScaScanResult>? = null

    init {
        thisLogger().info("CycodeResultsService init")
    }

    fun setSecretResults(result: CliResult<SecretScanResult>) {
        clearDetectedSegments(CliScanType.Secret)
        secretResults = result
    }

    fun getSecretResults(): CliResult<SecretScanResult>? {
        return secretResults
    }

    fun setScaResults(result: CliResult<ScaScanResult>) {
        clearDetectedSegments(CliScanType.Sca)
        scaResults = result
    }

    fun getScaResults(): CliResult<ScaScanResult>? {
        return scaResults
    }

    fun clear() {
        secretResults = null
        scaResults = null
        clearDetectedSegments()
    }

    fun hasResults(): Boolean {
        return secretResults != null || scaResults != null
    }

    fun saveDetectedSegment(scanType: CliScanType, textRange: TextRange, value: String) {
        detectedSegments[Pair(scanType, textRange)] = value
    }

    fun getDetectedSegment(scanType: CliScanType, textRange: TextRange): String? {
        return detectedSegments[Pair(scanType, textRange)]
    }

    private fun clearDetectedSegments(scanType: CliScanType? = null) {
        if (scanType == null) {
            detectedSegments.clear()
            return
        }

        detectedSegments.filter { it.key.first == scanType }.forEach { detectedSegments.remove(it.key) }
    }

    fun excludeResults(byValue: String? = null, byPath: String? = null, byRuleId: String? = null) {
        if (secretResults is CliResult.Success) {
            val filter = SecretScanResultsFilter((secretResults as CliResult.Success<SecretScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId)
            secretResults = CliResult.Success(filter.getFilteredScanResults())
        }
        // more scan types to be added here
    }
}
