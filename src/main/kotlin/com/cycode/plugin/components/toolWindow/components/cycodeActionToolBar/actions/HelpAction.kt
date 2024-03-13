package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.openURL
import com.cycode.plugin.components.toolWindow.CycodeContentTab
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class HelpAction :
    DumbAwareAction(Supplier { CycodeBundle.message("toolbarHelpAction") }, AllIcons.Actions.Help) {
    companion object {
        fun create(contentTab: CycodeContentTab): HelpAction {
            return HelpAction()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        openURL(CycodeBundle.message("docsUrl"))
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && !e.project!!.isDisposed
    }
}
