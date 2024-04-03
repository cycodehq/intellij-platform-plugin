package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab

import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.header.SecretHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.shortSummary.SecretShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.summary.SecretSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.title.SecretTitle
import javax.swing.JComponent

class SecretViolationCardContentTab : CommonViolationCardContentTab() {
    fun getContent(detection: SecretDetection): JComponent {
        val titlePanel = SecretTitle().getContent(detection)
        val shortSummaryPanel = SecretShortSummary().getContent(detection)
        val headerContentPanel = SecretHeader().addContent(detection)
        val summaryPanel = SecretSummary().getContent(detection)

        return getContent(listOf(titlePanel, shortSummaryPanel, headerContentPanel, summaryPanel))
    }
}