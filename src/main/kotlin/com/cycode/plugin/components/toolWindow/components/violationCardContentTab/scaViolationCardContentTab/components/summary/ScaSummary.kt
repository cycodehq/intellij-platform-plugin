package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class ScaSummary : CardHtmlSummary() {
    private fun getDescription(detection: ScaDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.alert?.description ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: ScaDetection): JComponent {
        return getContent(
            CycodeBundle.message("scaViolationCardSummaryTitle"),
            getDescription(detection)
        )
    }
}
