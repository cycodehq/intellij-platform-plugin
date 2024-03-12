package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.CycodeContentTab
import com.cycode.plugin.components.toolWindow.getRightPanelDependingOnState
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class HomeAction(private val contentTab: CycodeContentTab) :
    DumbAwareAction(Supplier { CycodeBundle.message("toolbarHomeAction") }, AllIcons.Actions.Back) {
    companion object {
        fun create(contentTab: CycodeContentTab): HomeAction {
            return HomeAction(contentTab)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val initRightPanel = getRightPanelDependingOnState(e.project!!)
        contentTab.updateContent(initRightPanel)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && !e.project!!.isDisposed
    }
}
