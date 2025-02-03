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

    var secretResults: CliResult<SecretScanResult>? = null
        set(value) {
            clearDetectedSegments(CliScanType.Secret)
            field = value
        }

    var scaResults: CliResult<ScaScanResult>? = null
        set(value) {
            clearDetectedSegments(CliScanType.Sca)
            field = value
        }

    var iacResults: CliResult<IacScanResult>? = null
        set(value) {
            clearDetectedSegments(CliScanType.Iac)
            field = value
        }

    var sastResults: CliResult<SastScanResult>? = null
        set(value) {
            clearDetectedSegments(CliScanType.Sast)
            field = value
        }

    init {
        thisLogger().info("CycodeResultsService init")
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

    fun excludeResults(
        byValue: String? = null, byPath: String? = null, byRuleId: String? = null, byCve: String? = null
    ) {
        if (secretResults is CliResult.Success) {
            val filter = SecretScanResultsFilter((secretResults as CliResult.Success<SecretScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId, byCve)
            secretResults = CliResult.Success(filter.getFilteredScanResults())
        }
        if (scaResults is CliResult.Success) {
            val filter = ScaScanResultsFilter((scaResults as CliResult.Success<ScaScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId, byCve)
            scaResults = CliResult.Success(filter.getFilteredScanResults())
        }
        if (iacResults is CliResult.Success) {
            val filter = IacScanResultsFilter((iacResults as CliResult.Success<IacScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId, byCve)
            iacResults = CliResult.Success(filter.getFilteredScanResults())
        }
        if (sastResults is CliResult.Success) {
            val filter = SastScanResultsFilter((sastResults as CliResult.Success<SastScanResult>).result)
            filter.exclude(byValue, byPath, byRuleId, byCve)
            sastResults = CliResult.Success(filter.getFilteredScanResults())
        }
    }
}
