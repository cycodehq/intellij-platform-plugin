package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.title

import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.title.CardTitle
import javax.swing.JComponent

class ScaTitle : CardTitle() {
    fun getContent(detection: ScaDetection): JComponent {
        return getContent(detection.severity, detection.detectionDetails.alert?.summary ?: detection.message)
    }
}
