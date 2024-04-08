package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.companyGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.summary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class IacCompanyGuidelines : CardHtmlSummary() {
    private fun getCustomGuidelines(detection: IacDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.customRemediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: IacDetection): JComponent {
        return getContent(
            CycodeBundle.message("iacViolationCardCompanyGuidelinesTitle"),
            getCustomGuidelines(detection)
        )
    }
}
