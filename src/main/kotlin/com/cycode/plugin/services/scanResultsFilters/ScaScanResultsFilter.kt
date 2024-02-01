package com.cycode.plugin.services.scanResultsFilters

import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaScanResult

class ScaScanResultsFilter(scanResults: ScaScanResult) : ScanResultsFilterBase<ScaScanResult>(scanResults) {
    private var filteredScanResults = scanResults

    private fun filter(predicative: (ScaDetection) -> Boolean) {
        filteredScanResults = ScaScanResult(
            detections = scanResults.detections.filter(predicative),
            errors = scanResults.errors,
        )
    }

    override fun excludeByValue(value: String) {
        // do nothing because we don't have a value field in SCA
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

    override fun getFilteredScanResults(): ScaScanResult {
        return filteredScanResults
    }
}
