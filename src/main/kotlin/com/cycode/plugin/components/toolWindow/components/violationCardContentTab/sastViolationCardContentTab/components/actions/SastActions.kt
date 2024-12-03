package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.actions.CardActions
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import com.cycode.plugin.services.cycode
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SastActions(val project: Project) : CardActions() {
    fun addContent(detection: SastDetection, aiRemediationComponent: CardHtmlSummary): JComponent {
        addActionButton(CycodeBundle.message("generateAiRemediationBtn"), onClick = {
            cycode(project).getAiRemediation(detection.id) { remediationResult ->
                aiRemediationComponent.setHtmlContent(convertMarkdownToHtml(remediationResult.remediation))
            }
        })

        return getContent()
    }
}
