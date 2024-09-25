package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.summary.CardSummary
import javax.swing.JComponent

class SecretSummary : CardSummary() {
    fun getContent(detection: SecretDetection): JComponent {
        val title = CycodeBundle.message("violationCardSummaryTitle")
        val message = detection.detectionDetails.description ?: detection.getFormattedMessage()

        return getContent(title, message)
    }
}
