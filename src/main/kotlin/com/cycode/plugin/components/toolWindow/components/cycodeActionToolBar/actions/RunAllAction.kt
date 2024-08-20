package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.pluginState
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class RunAllAction :
    DumbAwareAction(Supplier { CycodeBundle.message("toolbarRunAllAction") }, AllIcons.Actions.RunAll) {
    companion object {
        fun create(): RunAllAction {
            return RunAllAction()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        // TODO(MarshalX): we should tracks all running scans and use pub-sub messaging to update UI
        //  we can provide "stop" action only after that
        val project = e.project ?: return
        val service = cycode(project)

        service.startSecretScanForCurrentProject()
        service.startScaScanForCurrentProject()
        service.startIacScanForCurrentProject()
        service.startSastScanForCurrentProject()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null &&
                !e.project!!.isDisposed &&
                pluginState().cliAuthed
    }
}
