package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.cycodeGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class IacCycodeGuidelines : CardHtmlSummary(CycodeBundle.message("violationCardCycodeGuidelinesTitle")) {
    private fun getCycodeGuidelines(detection: IacDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.remediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: IacDetection): JComponent {
        return getContent(getCycodeGuidelines(detection))
    }
}
