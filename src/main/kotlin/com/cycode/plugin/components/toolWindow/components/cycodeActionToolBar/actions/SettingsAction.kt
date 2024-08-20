package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.settings.ApplicationSettingsConfigurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class SettingsAction :
    DumbAwareAction(Supplier { CycodeBundle.message("toolbarSettingsAction") }, AllIcons.General.GearPlain) {
    companion object {
        fun create(): SettingsAction {
            return SettingsAction()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project, ApplicationSettingsConfigurable::class.java
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && !e.project!!.isDisposed
    }
}
