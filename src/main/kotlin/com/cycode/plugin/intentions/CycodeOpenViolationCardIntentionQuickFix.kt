package com.cycode.plugin.intentions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.components.toolWindow.CycodeToolWindowFactory
import com.cycode.plugin.components.toolWindow.activateToolWindow
import com.cycode.plugin.icons.PluginIcons
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon


class CycodeOpenViolationCardIntentionQuickFix(
    private val detection: DetectionBase,
) :
    BaseIntentionAction(), PriorityAction, Iconable {
    override fun getText(): String {
        var detectionHashCode = detection.hashCode()
        if (detectionHashCode < 0) detectionHashCode *= -1 // make positive

        val detectionUniqueId = detectionHashCode
            .toString(16) // convert to hex
            .slice(0..5) // take the first 6 characters

        var text = detection.getFormattedMessage()
        // cut too long messages
        if (text.length > 50) {
            text = text.substring(0, 50) + "..."
        }

        return CycodeBundle.message(
            "violationCardIntentionText", text, detectionUniqueId
        )
    }

    override fun getFamilyName(): String {
        return CycodeBundle.message("violationCardIntentionFamilyName")
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null || file != file.originalFile) {
            // Disable Intention Action Preview
            return
        }

        thisLogger().warn("Open violation card quick fix intention has been invoked")

        CycodeToolWindowFactory.TabManager.getTab(project)
            ?.getTreeView()
            ?.displayViolationCard(detection)

        activateToolWindow(project)
    }

    override fun getPriority(): PriorityAction.Priority {
        return when (detection.severity.toLowerCase()) {
            "critical" -> PriorityAction.Priority.TOP
            "high" -> PriorityAction.Priority.HIGH
            "medium" -> PriorityAction.Priority.NORMAL
            else -> PriorityAction.Priority.LOW
        }
    }

    override fun getIcon(flags: Int): Icon {
        return PluginIcons.getSeverityIcon(detection.severity)
    }
}
