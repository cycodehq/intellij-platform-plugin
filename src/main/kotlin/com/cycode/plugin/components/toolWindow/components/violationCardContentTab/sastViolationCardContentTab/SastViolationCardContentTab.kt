package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.actions.SastActions
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.companyGuidelines.SastCompanyGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.cycodeGuidelines.SastCycodeGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.header.SastHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.shortSummary.SastShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.summary.SastSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.title.SastTitle
import com.cycode.plugin.services.pluginState
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SastViolationCardContentTab(val project: Project) : CommonViolationCardContentTab() {
    fun getContent(detection: SastDetection): JComponent {
        val pluginState = pluginState()

        val titlePanel = SastTitle().getContent(detection)
        val shortSummaryPanel = SastShortSummary().getContent(detection)
        val headerContentPanel = SastHeader().addContent(detection)
        val summaryPanel = SastSummary().getContent(detection)
        val companyGuidelines = SastCompanyGuidelines().getContent(detection)
        val cycodeGuidelines = SastCycodeGuidelines().getContent(detection)

        val componentsToRender = mutableListOf(
            titlePanel,
            shortSummaryPanel,
            headerContentPanel,
            summaryPanel,
            companyGuidelines,
            cycodeGuidelines,
        )

        if (pluginState.isAiLargeLanguageModelEnabled) {
            val aiRemediationComponent = CardHtmlSummary(CycodeBundle.message("violationCardAiRemediationTitle"))
            val actionsPanel = SastActions(project).addContent(detection, aiRemediationComponent)
            componentsToRender.add(aiRemediationComponent.getContent())
            componentsToRender.add(actionsPanel)
        }

        return getContent(componentsToRender)
    }
}
