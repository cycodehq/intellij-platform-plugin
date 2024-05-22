package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.shortSummary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary.CardShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.renderCweCveLink
import javax.swing.JComponent

class ScaShortSummary : CardShortSummary() {
    fun getContent(detection: ScaDetection): JComponent {
        if (detection.detectionDetails.alert != null) {
            val severity = detection.severity

            val cwe = detection.detectionDetails.vulnerabilityId
            val renderedCwe = renderCweCveLink(cwe)

            val shortSummary = CycodeBundle.message("scaViolationCardShortSummary", severity, renderedCwe)

            return getContent(shortSummary)
        }

        return getContent()
    }
}
