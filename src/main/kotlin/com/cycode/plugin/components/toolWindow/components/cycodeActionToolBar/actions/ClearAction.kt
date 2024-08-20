package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.CycodeContentTab
import com.cycode.plugin.services.scanResults
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class ClearAction(private val contentTab: CycodeContentTab) :
    DumbAwareAction(Supplier { CycodeBundle.message("toolbarClearAction") }, AllIcons.Actions.GC) {
    companion object {
        fun create(contentTab: CycodeContentTab): ClearAction {
            return ClearAction(contentTab)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        scanResults(project).clear()
        contentTab.getTreeView().refreshTree()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null &&
                !e.project!!.isDisposed &&
                scanResults(e.project!!).hasResults()
    }
}
