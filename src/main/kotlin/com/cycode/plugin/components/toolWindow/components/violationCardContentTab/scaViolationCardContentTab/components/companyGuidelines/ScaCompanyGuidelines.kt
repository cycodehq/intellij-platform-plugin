package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.companyGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class ScaCompanyGuidelines : CardHtmlSummary(CycodeBundle.message("violationCardCompanyGuidelinesTitle")) {
    private fun getCustomGuidelines(detection: ScaDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.customRemediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: ScaDetection): JComponent {
        return getContent(getCustomGuidelines(detection))
    }
}
