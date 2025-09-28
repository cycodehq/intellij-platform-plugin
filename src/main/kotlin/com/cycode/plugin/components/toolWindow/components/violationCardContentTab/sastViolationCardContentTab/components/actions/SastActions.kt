package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.actions.CardActions
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary.CardHtmlSummary
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.convertMarkdownToHtml
import com.cycode.plugin.services.cycode
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import javax.swing.SwingUtilities

class SastActions(val project: Project) : CardActions() {

    companion object {
        private const val GENERATE_AI_REMEDIATION_ID = "generate_ai_remediation"
        private const val APPLY_AI_REMEDIATION_ID = "apply_ai_remediation"
    }

    fun addContent(detection: SastDetection, aiRemediationComponent: CardHtmlSummary): JComponent {
        val generateButtonText = CycodeBundle.message("generateAiRemediationBtn")
        val applyButtonText = CycodeBundle.message("applyAiRemediationBtn")

        addActionButton(
            id = GENERATE_AI_REMEDIATION_ID,
            text = generateButtonText,
            onClick = {
                cycode(project).getAiRemediation(
                    detectionId = detection.id,
                    onSuccess = { remediationResult ->
                        aiRemediationComponent.setHtmlContent(convertMarkdownToHtml(remediationResult.remediation))

                        SwingUtilities.invokeLater {
                            hideButton(GENERATE_AI_REMEDIATION_ID)

                            if (remediationResult.isFixAvailable) {
                                showButton(APPLY_AI_REMEDIATION_ID)
                            }
                        }
                    },
                    onFailure = {
                        enableButton(GENERATE_AI_REMEDIATION_ID)
                    }
                )
            },
            async = true,
            inProgressText = CycodeBundle.message("generateAiRemediationBtnInProgress")
        )

        addActionButton(
            id = APPLY_AI_REMEDIATION_ID,
            text = applyButtonText,
            onClick = {
                // TODO: Implement actual apply fix logic
                Thread.sleep(3000)
            },
            inProgressText = CycodeBundle.message("applyAiRemediationBtnInProgress")
        )
        hideButton(APPLY_AI_REMEDIATION_ID)

        return getContent()
    }
}
