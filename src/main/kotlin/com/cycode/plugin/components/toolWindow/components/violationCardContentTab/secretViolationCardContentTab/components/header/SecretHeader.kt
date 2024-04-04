package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.header

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.header.CardHeader
import javax.swing.JComponent

class SecretHeader : CardHeader() {
    fun addContent(detection: SecretDetection): JComponent {
        addHeader(CycodeBundle.message("secretViolationCardHeaderRuleIdField"), detection.detectionRuleId)
        addHeader(CycodeBundle.message("secretViolationCardHeaderFileField"), detection.detectionDetails.fileName)
        addHeader(CycodeBundle.message("secretViolationCardHeaderShaField"), detection.detectionDetails.sha512)

        return getContent()
    }
}
