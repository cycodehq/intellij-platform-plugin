package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.title

import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.title.CardTitle
import javax.swing.JComponent

class IacTitle : CardTitle() {
    fun getContent(detection: IacDetection): JComponent {
        return getContent(detection.severity, detection.message)
    }
}
