package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab

import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.companyGuidelines.IacCompanyGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.cycodeGuidelines.IacCycodeGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.header.IacHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.shortSummary.IacShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.summary.IacSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.title.IacTitle
import javax.swing.JComponent

class IacViolationCardContentTab : CommonViolationCardContentTab() {
    fun getContent(detection: IacDetection): JComponent {
        val titlePanel = IacTitle().getContent(detection)
        val shortSummaryPanel = IacShortSummary().getContent(detection)
        val headerContentPanel = IacHeader().addContent(detection)
        val summaryPanel = IacSummary().getContent(detection)
        val companyGuidelines = IacCompanyGuidelines().getContent(detection)
        val cycodeGuidelines = IacCycodeGuidelines().getContent(detection)

        return getContent(
            listOf(
                titlePanel,
                shortSummaryPanel,
                headerContentPanel,
                summaryPanel,
                companyGuidelines,
                cycodeGuidelines
            )
        )
    }
}
