package com.cycode.plugin.intentions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.scanResults
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon


class CycodeIgnoreIntentionQuickFix(
    private val scanType: CliScanType,
    private val type: CycodeIgnoreType,
    private val value: String
) :
    BaseIntentionAction(), PriorityAction, Iconable {
    override fun getText(): String {
        with(type) {
            return when (this) {
                CycodeIgnoreType.VALUE -> CycodeBundle.message("ignoreIntentionByValueText", value)
                CycodeIgnoreType.RULE -> CycodeBundle.message("ignoreIntentionByRuleText", value)
                CycodeIgnoreType.PATH -> CycodeBundle.message("ignoreIntentionByPathText", value)
            }
        }
    }

    override fun getFamilyName(): String {
        return CycodeBundle.message("ignoreIntentionFamilyName")
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    private fun mapTypeToOptionName(type: CycodeIgnoreType): String {
        return when (type) {
            CycodeIgnoreType.VALUE -> "--by-value"
            CycodeIgnoreType.RULE -> "--by-rule"
            CycodeIgnoreType.PATH -> "--by-path"
        }
    }

    private fun applyIgnoreInUi(project: Project) {
        // exclude results from the local DB and restart the code analyzer

        val scanResults = scanResults(project)
        when (type) {
            CycodeIgnoreType.VALUE -> scanResults.excludeResults(byValue = value)
            CycodeIgnoreType.RULE -> scanResults.excludeResults(byRuleId = value)
            CycodeIgnoreType.PATH -> scanResults.excludeResults(byPath = value)
        }

        DaemonCodeAnalyzer.getInstance(project).restart()
        updateToolWindowState(project)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null || file != file.originalFile) {
            /**
             * since 2022.3, JB introduced "Intention Action Preview" feature
             * which breaks our ignore options
             * because it performs quick fix intention on a copy of the file on hover
             * see https://plugins.jetbrains.com/docs/intellij/code-intentions-preview.html
             */
            thisLogger().debug("skip quick fix intention for virtually copied files")
            return
        }

        thisLogger().warn("Ignore quick fix intention has been invoked")

        // we are removing is from UI first to show how it's blazing fast and then apply it in the background
        applyIgnoreInUi(project)

        cycode(project).applyIgnoreFromFileAnnotation(scanType.name.toLowerCase(), mapTypeToOptionName(type), value)
    }

    override fun getPriority(): PriorityAction.Priority {
        return PriorityAction.Priority.LOW
    }

    override fun getIcon(flags: Int): Icon {
        return com.intellij.icons.AllIcons.Actions.IntentionBulb
    }
}
