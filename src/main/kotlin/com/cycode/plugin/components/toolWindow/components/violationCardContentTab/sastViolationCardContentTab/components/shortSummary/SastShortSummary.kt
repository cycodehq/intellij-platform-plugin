package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.shortSummary

import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary.CardShortSummary
import javax.swing.JComponent

class SastShortSummary : CardShortSummary() {
    fun getContent(detection: SastDetection): JComponent {
        var shortSummary = detection.severity

        val cwe = detection.detectionDetails.cwe.joinToString(", ")
        if (cwe.isNotEmpty()) {
            shortSummary += " | $cwe"
        }

        return getContent(shortSummary)
    }
}
