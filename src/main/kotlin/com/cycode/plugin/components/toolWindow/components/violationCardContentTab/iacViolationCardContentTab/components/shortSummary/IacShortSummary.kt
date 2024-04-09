package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.shortSummary

import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary.CardShortSummary
import javax.swing.JComponent

class IacShortSummary : CardShortSummary() {
    fun getContent(detection: IacDetection): JComponent {
        return getContent(detection.severity)
    }
}
