package com.cycode.plugin.services.scanResultsFilters

import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.iac.IacScanResult

class IacScanResultsFilter(scanResults: IacScanResult) : ScanResultsFilterBase<IacScanResult>(scanResults) {
    private var filteredScanResults = scanResults

    private fun filter(predicative: (IacDetection) -> Boolean) {
        filteredScanResults = IacScanResult(
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

    override fun getFilteredScanResults(): IacScanResult {
        return filteredScanResults
    }
}
