package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.shortSummary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary.CardShortSummary
import javax.swing.JComponent

class ScaShortSummary : CardShortSummary() {
    fun getContent(detection: ScaDetection): JComponent {
        if (detection.detectionDetails.alert != null) {
            val cwe = detection.detectionDetails.vulnerabilityId
            val severity = detection.severity
            val shortSummary = CycodeBundle.message("scaViolationCardShortSummary", severity, cwe ?: "")

            return getContent(shortSummary)
        }

        return getContent()
    }
}
