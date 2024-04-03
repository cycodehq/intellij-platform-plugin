package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.summary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class SecretSummary : CardHtmlSummary() {
    private fun getCustomGuideline(detection: SecretDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.customRemediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: SecretDetection): JComponent {
        return getContent(
            CycodeBundle.message("secretViolationCardSummaryTitle"),
            getCustomGuideline(detection)
        )
    }
}
