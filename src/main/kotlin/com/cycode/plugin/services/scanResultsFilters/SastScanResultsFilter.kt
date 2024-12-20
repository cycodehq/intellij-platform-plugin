package com.cycode.plugin.services.scanResultsFilters

import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.cli.models.scanResult.sast.SastScanResult

class SastScanResultsFilter(scanResults: SastScanResult) : ScanResultsFilterBase<SastScanResult>(scanResults) {
    private var filteredScanResults = scanResults

    private fun filter(predicative: (SastDetection) -> Boolean) {
        filteredScanResults = SastScanResult(
            detections = scanResults.detections.filter(predicative),
            errors = scanResults.errors,
        )
    }

    override fun excludeByValue(value: String) {
        // do nothing because we don't have a value field in SAST
    }

    override fun excludeByPath(path: String) {
        filter { detection ->
            detection.detectionDetails.getFilepath() != path
        }
    }

    override fun excludeByRuleId(ruleId: String) {
        filter { detection ->
            detection.detectionRuleId != ruleId
        }
    }

    override fun excludeByCve(cve: String) {
        // do nothing because we don't have a value field in SAST
    }

    override fun getFilteredScanResults(): SastScanResult {
        return filteredScanResults
    }
}
