package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.companyGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class SastCompanyGuidelines : CardHtmlSummary(CycodeBundle.message("violationCardCompanyGuidelinesTitle")) {
    private fun getCustomGuidelines(detection: SastDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.customRemediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: SastDetection): JComponent {
        return getContent(getCustomGuidelines(detection))
    }
}
