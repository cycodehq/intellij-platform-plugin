package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab

import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.header.SastHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.shortSummary.SastShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.summary.SastSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.title.SastTitle
import javax.swing.JComponent

class SastViolationCardContentTab : CommonViolationCardContentTab() {
    fun getContent(detection: SastDetection): JComponent {
        val titlePanel = SastTitle().getContent(detection)
        val shortSummaryPanel = SastShortSummary().getContent(detection)
        val headerContentPanel = SastHeader().addContent(detection)
        val summaryPanel = SastSummary().getContent(detection)

        return getContent(
            listOf(
                titlePanel,
                shortSummaryPanel,
                headerContentPanel,
                summaryPanel,
            )
        )
    }
}
