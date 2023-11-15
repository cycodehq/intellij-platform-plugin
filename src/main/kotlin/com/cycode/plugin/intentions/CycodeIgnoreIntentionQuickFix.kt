package com.cycode.plugin.intentions

import com.cycode.plugin.services.cycode
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon

enum class CycodeIgnoreType {
    VALUE,
    RULE,
    PATH
}


class CycodeIgnoreIntentionQuickFix(private val type: CycodeIgnoreType, private val value: String) :
    BaseIntentionAction(), PriorityAction, Iconable {
    override fun getText(): String {
        with(type) {
            return when (this) {
                CycodeIgnoreType.VALUE -> "Ignore value '$value'"
                CycodeIgnoreType.RULE -> "Ignore rule '$value'"
                CycodeIgnoreType.PATH -> "Ignore path '$value'"
            }
        }
    }

    override fun getFamilyName(): String {
        return "Ignore"
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

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        thisLogger().warn("Ignore quick fix intention has been invoked")

        val filepath = file?.virtualFile?.path ?: return
        cycode(project).applyIgnoreFromFileAnnotation(filepath, mapTypeToOptionName(type), value)
    }

    override fun getPriority(): PriorityAction.Priority {
        return PriorityAction.Priority.LOW
    }

    override fun getIcon(flags: Int): Icon {
        return com.intellij.icons.AllIcons.Actions.IntentionBulb
    }
}
