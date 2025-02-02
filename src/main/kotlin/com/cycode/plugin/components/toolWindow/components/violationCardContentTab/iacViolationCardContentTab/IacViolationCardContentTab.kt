package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.actions.IacActions
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.companyGuidelines.IacCompanyGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.cycodeGuidelines.IacCycodeGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.header.IacHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.shortSummary.IacShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.summary.IacSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.title.IacTitle
import com.cycode.plugin.services.pluginLocalState
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class IacViolationCardContentTab(val project: Project) : CommonViolationCardContentTab() {
    fun getContent(detection: IacDetection): JComponent {
        val pluginLocalState = pluginLocalState(project)

        val titlePanel = IacTitle().getContent(detection)
        val shortSummaryPanel = IacShortSummary().getContent(detection)
        val headerContentPanel = IacHeader().addContent(detection)
        val summaryPanel = IacSummary().getContent(detection)
        val companyGuidelines = IacCompanyGuidelines().getContent(detection)
        val cycodeGuidelines = IacCycodeGuidelines().getContent(detection)

        val componentsToRender = mutableListOf(
            titlePanel,
            shortSummaryPanel,
            headerContentPanel,
            summaryPanel,
            companyGuidelines,
            cycodeGuidelines,
        )

        if (pluginLocalState.isAiLargeLanguageModelEnabled) {
            val aiRemediationComponent = CardHtmlSummary(CycodeBundle.message("violationCardAiRemediationTitle"))
            val actionsPanel = IacActions(project).addContent(detection, aiRemediationComponent)
            componentsToRender.add(aiRemediationComponent.getContent())
            componentsToRender.add(actionsPanel)
        }

        return getContent(componentsToRender)
    }
}
