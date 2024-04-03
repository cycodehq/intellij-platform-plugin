package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.title

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.title.CardTitle
import javax.swing.JComponent

class SecretTitle : CardTitle() {
    fun getContent(detection: SecretDetection): JComponent {
        return getContent(detection.severity, CycodeBundle.message("secretViolationCardTitle", detection.type))
    }
}
