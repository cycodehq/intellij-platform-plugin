package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab

import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.companyGuidelines.ScaCompanyGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.cycodeGuidelines.ScaCycodeGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.header.ScaHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.shortSummary.ScaShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.summary.ScaSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.title.ScaTitle
import javax.swing.JComponent

class ScaViolationCardContentTab : CommonViolationCardContentTab() {
    fun getContent(detection: ScaDetection): JComponent {
        val titlePanel = ScaTitle().getContent(detection)
        val shortSummaryPanel = ScaShortSummary().getContent(detection)
        val headerContentPanel = ScaHeader().addContent(detection)
        val companyGuidelines = ScaCompanyGuidelines().getContent(detection)
        val cycodeGuidelines = ScaCycodeGuidelines().getContent(detection)
        val summaryPanel = ScaSummary().getContent(detection)

        return getContent(
            listOf(
                titlePanel,
                shortSummaryPanel,
                headerContentPanel,
                summaryPanel,
                companyGuidelines,
                cycodeGuidelines,
            )
        )
    }
}
