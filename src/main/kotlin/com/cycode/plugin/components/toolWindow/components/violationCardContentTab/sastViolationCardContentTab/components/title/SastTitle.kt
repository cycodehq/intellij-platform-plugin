package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.title

import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.title.CardTitle
import javax.swing.JComponent

class SastTitle : CardTitle() {
    fun getContent(detection: SastDetection): JComponent {
        return getContent(detection.severity, detection.getFormattedMessage())
    }
}
