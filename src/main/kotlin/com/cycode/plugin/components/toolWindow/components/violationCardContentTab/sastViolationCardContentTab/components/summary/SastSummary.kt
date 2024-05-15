package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import javax.swing.JComponent

class SastSummary : CardHtmlSummary() {
    private fun getSummary(detection: SastDetection): String {
        return convertMarkdownToHtml(detection.detectionDetails.description)
    }

    fun getContent(detection: SastDetection): JComponent {
        return getContent(CycodeBundle.message("sastViolationCardSummaryTitle"), getSummary(detection))
    }
}
