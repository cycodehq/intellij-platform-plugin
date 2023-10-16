package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.toolWindow.components.authContentTab.AuthContentTab
import com.cycode.plugin.components.toolWindow.components.loadingContentTab.LoadingContentTab
import com.cycode.plugin.components.toolWindow.components.scanContentTab.ScanContentTab
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.CycodeService
import com.cycode.plugin.services.pluginState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory


class CycodeToolWindowFactory : DumbAware, ToolWindowFactory {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(PluginIcons.TOOL_WINDOW)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createLoadingTabOnly(project)
    }

    override fun shouldBeAvailable(project: Project) = true
}

private fun getCycodeToolWindow(project: Project): ToolWindow? {
    return ToolWindowManager.getInstance(project).getToolWindow(CycodeBundle.message("toolWindowId"))
}

private fun replaceToolWindowContent(project: Project, content: Content) {
    val window = getCycodeToolWindow(project) ?: return
    window.contentManager.removeAllContents(true)
    window.contentManager.addContent(content)
}

private fun createToolWindowContent(project: Project, component: Component<CycodeService>): Content {
    val service = project.service<CycodeService>()

    return ContentFactory.SERVICE.getInstance()
        .createContent(component.getContent(service), null, false)
}

private fun createLoadingTabOnly(project: Project) {
    replaceToolWindowContent(project, createToolWindowContent(project, LoadingContentTab()))
}


private fun createAuthTabOnly(project: Project) {
    replaceToolWindowContent(project, createToolWindowContent(project, AuthContentTab()))
}

private fun createScanTabOnly(project: Project) {
    replaceToolWindowContent(project, createToolWindowContent(project, ScanContentTab()))
}

fun updateToolWindowState(project: Project) {
    val pluginState = pluginState()
    ApplicationManager.getApplication().invokeLater {
        WriteAction.run<Error> {
            if (pluginState.cliAuthed) {
                createScanTabOnly(project)
            } else {
                createAuthTabOnly(project)
            }
        }
    }
}
