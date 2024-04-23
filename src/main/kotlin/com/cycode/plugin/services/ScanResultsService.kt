package com.cycode.plugin.services

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.iac.IacScanResult
import com.cycode.plugin.cli.models.scanResult.sast.SastScanResult
import com.cycode.plugin.cli.models.scanResult.sca.ScaScanResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.services.scanResultsFilters.IacScanResultsFilter
import com.cycode.plugin.services.scanResultsFilters.SastScanResultsFilter
import com.cycode.plugin.services.scanResultsFilters.ScaScanResultsFilter
import com.cycode.plugin.services.scanResultsFilters.SecretScanResultsFilter
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange

@Service(Service.Level.PROJECT)
class ScanResultsService {
    private val detectedSegments = mutableMapOf<Pair<CliScanType, TextRange>, String>()

    private var secretResults: CliResult<SecretScanResult>? = null
    private var scaResults: CliResult<ScaScanResult>? = null
    private var iacResults: CliResult<IacScanResult>? = null
    private var sastResults: CliResult<SastScanResult>? = null

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

    fun setIacResults(result: CliResult<IacScanResult>) {
        clearDetectedSegments(CliScanType.Iac)
        iacResults = result
    }

    fun getIacResults(): CliResult<IacScanResult>? {
        return iacResults
    }

    fun setSastResults(result: CliResult<SastScanResult>) {
        clearDetectedSegments(CliScanType.Sast)
        sastResults = result
    }

    fun getSastResults(): CliResult<SastScanResult>? {
        return sastResults
    }

    fun clear() {
        secretResults = null
        scaResults = null
        iacResults = null
        sastResults = null
        clearDetectedSegments()
    }

    fun hasResults(): Boolean {
        return secretResults != null || scaResults != null || iacResults != null || sastResults != null
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
        if (scaResults is CliResult.Success) {
            val filter = ScaScanResultsFilter((scaResults as CliResult.Success<ScaScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId)
            scaResults = CliResult.Success(filter.getFilteredScanResults())
        }
        if (iacResults is CliResult.Success) {
            val filter = IacScanResultsFilter((iacResults as CliResult.Success<IacScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId)
            iacResults = CliResult.Success(filter.getFilteredScanResults())
        }
        if (sastResults is CliResult.Success) {
            val filter = SastScanResultsFilter((sastResults as CliResult.Success<SastScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId)
            sastResults = CliResult.Success(filter.getFilteredScanResults())
        }
    }
}
