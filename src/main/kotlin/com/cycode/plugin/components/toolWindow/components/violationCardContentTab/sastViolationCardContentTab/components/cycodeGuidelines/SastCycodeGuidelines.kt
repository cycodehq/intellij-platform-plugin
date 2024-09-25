package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.cycodeGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class SastCycodeGuidelines : CardHtmlSummary() {
    private fun getCycodeGuidelines(detection: SastDetection): String? {
        val descriptionMarkdown = detection.detectionDetails.remediationGuidelines ?: return null
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: SastDetection): JComponent {
        return getContent(
            CycodeBundle.message("violationCardCycodeGuidelinesTitle"),
            getCycodeGuidelines(detection)
        )
    }
}