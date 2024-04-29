package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.header

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.header.CardHeader
import java.io.File
import javax.swing.JComponent

class IacHeader : CardHeader() {
    fun addContent(detection: IacDetection): JComponent {
        addHeader(
            CycodeBundle.message("iacViolationCardHeaderFileField"),
            File(detection.detectionDetails.fileName).name
        )
        addHeader(CycodeBundle.message("iacViolationCardHeaderProviderField"), detection.detectionDetails.infraProvider)
        addHeader(CycodeBundle.message("iacViolationCardHeaderRuleIdField"), detection.detectionRuleId)

        return getContent()
    }
}
