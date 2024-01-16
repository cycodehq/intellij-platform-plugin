package com.cycode.plugin.services.scanResultsFilters

import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult

class SecretScanResultsFilter(scanResults: SecretScanResult) : ScanResultsFilterBase<SecretScanResult>(scanResults) {
    private var filteredScanResults = scanResults

    private fun filter(predicative: (SecretDetection) -> Boolean) {
        filteredScanResults = SecretScanResult(
            detections = scanResults.detections.filter(predicative),
            errors = scanResults.errors,
        )
    }

    override fun excludeByValue(value: String) {
        filter { detection ->
            detection.detectionDetails.detectedValue != value
        }
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

    override fun getFilteredScanResults(): SecretScanResult {
        return filteredScanResults
    }
}
