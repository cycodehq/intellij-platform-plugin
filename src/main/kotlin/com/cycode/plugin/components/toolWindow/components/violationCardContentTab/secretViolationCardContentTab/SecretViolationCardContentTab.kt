package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab

import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.CommonViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.actions.SecretActions
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.companyGuidelines.SecretCompanyGuidelines
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.header.SecretHeader
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.shortSummary.SecretShortSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.summary.SecretSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.title.SecretTitle
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SecretViolationCardContentTab(val project: Project) : CommonViolationCardContentTab() {
    fun getContent(detection: SecretDetection): JComponent {
        val titlePanel = SecretTitle().getContent(detection)
        val shortSummaryPanel = SecretShortSummary().getContent(detection)
        val headerContentPanel = SecretHeader().addContent(detection)
        val summaryPanel = SecretSummary().getContent(detection)
        val companyGuidelines = SecretCompanyGuidelines().getContent(detection)
        val actionsPanel = SecretActions(project).addContent(detection)

        return getContent(
            listOf(
                titlePanel,
                shortSummaryPanel,
                headerContentPanel,
                summaryPanel,
                companyGuidelines,
                actionsPanel,
            )
        )
    }
}
