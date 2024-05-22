package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.shortSummary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary.CardShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.renderCweCveLink
import javax.swing.JComponent

class SastShortSummary : CardShortSummary() {
    fun getContent(detection: SastDetection): JComponent {
        var shortSummary = detection.severity

        val renderedCwe = detection.detectionDetails.cwe.map { renderCweCveLink(it) }
        val cwe = renderedCwe.joinToString(", ")
        if (cwe.isNotEmpty()) {
            shortSummary = CycodeBundle.message("sastViolationCardShortSummary", shortSummary, cwe)
        }

        return getContent(shortSummary)
    }
}
