package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.summary.CardSummary
import javax.swing.JComponent

class IacSummary : CardSummary() {
    fun getContent(detection: IacDetection): JComponent {
        val title = CycodeBundle.message("iacViolationCardSummaryTitle")
        val message = detection.detectionDetails.description ?: detection.message

        return getContent(title, message)
    }
}
