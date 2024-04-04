package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.shortSummary

import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary.CardShortSummary
import javax.swing.JComponent

class SecretShortSummary : CardShortSummary() {
    fun getContent(detection: SecretDetection): JComponent {
        return getContent(detection.severity)
    }
}
