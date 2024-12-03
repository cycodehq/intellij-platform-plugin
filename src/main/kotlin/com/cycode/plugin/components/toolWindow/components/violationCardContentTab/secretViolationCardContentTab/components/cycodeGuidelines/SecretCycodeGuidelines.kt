package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.cycodeGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class SecretCycodeGuidelines : CardHtmlSummary(CycodeBundle.message("violationCardCycodeGuidelinesTitle")) {
    private fun getCycodeGuidelines(detection: SecretDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.remediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: SecretDetection): JComponent {
        return getContent(getCycodeGuidelines(detection))
    }
}
