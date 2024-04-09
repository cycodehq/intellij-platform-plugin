package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.summary.CardSummary
import javax.swing.JComponent

class SecretSummary : CardSummary() {
    fun getContent(detection: SecretDetection): JComponent {
        val title = CycodeBundle.message("secretViolationCardSummaryTitle")
        val fixedMessage = detection.message.replace(
            "within '' repository", // BE bug
            ""
        )
        val message = detection.detectionDetails.description ?: fixedMessage

        return getContent(title, message)
    }
}
