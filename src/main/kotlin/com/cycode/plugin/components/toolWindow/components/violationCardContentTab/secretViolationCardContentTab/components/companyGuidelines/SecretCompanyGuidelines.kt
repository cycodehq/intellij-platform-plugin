package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.companyGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class SecretCompanyGuidelines : CardHtmlSummary() {
    private fun getCustomGuidelines(detection: SecretDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.customRemediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: SecretDetection): JComponent {
        return getContent(
            CycodeBundle.message("secretViolationCardCompanyGuidelinesTitle"),
            getCustomGuidelines(detection)
        )
    }
}
