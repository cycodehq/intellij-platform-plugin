package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.components.toolWindow.components.authContentTab.AuthContentTab
import com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.CycodeActionToolbar
import com.cycode.plugin.components.toolWindow.components.loadingContentTab.LoadingContentTab
import com.cycode.plugin.components.toolWindow.components.scanContentTab.ScanContentTab
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.pluginState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel

class CycodeToolWindowFactory : DumbAware, ToolWindowFactory {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(PluginIcons.TOOL_WINDOW)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = cycode(project)

        val contentTab = CycodeContentTab(project)
        TabManager.addTab(project, contentTab)

        CycodeActionToolbar().attachActionToolbar(contentTab)

        val initRightPanel = getRightPanelDependingOnState(project)
        contentTab.updateContent(initRightPanel)

        val toolWindowContent = createToolWindowContent(contentTab)
        toolWindow.contentManager.addContent(toolWindowContent)

        Disposer.register(service, toolWindowContent)
    }

    override fun shouldBeAvailable(project: Project) = true

    object TabManager {
        private val toolWindowsTabs = mutableMapOf<Project, CycodeContentTab>()

        fun addTab(project: Project, tab: CycodeContentTab) {
            toolWindowsTabs[project] = tab
        }

        fun getTab(project: Project): CycodeContentTab? {
            return toolWindowsTabs[project]
        }

        fun removeTab(project: Project) {
            toolWindowsTabs.remove(project)
        }
    }

}

private fun replaceToolWindowRightPanel(project: Project, panel: JPanel) {
    val contentTab = CycodeToolWindowFactory.TabManager.getTab(project) ?: return
    contentTab.updateContent(panel)
}

private fun createToolWindowContent(component: JPanel): Content {
    return ContentFactory.SERVICE.getInstance().createContent(component, null, false)
}

fun getRightPanelDependingOnState(project: Project): JPanel {
    val service = cycode(project)
    val pluginState = pluginState()

    if (!pluginState.cliInstalled) {
        return LoadingContentTab().getContent(service)
    }

    return if (pluginState.cliAuthed) {
        ScanContentTab().getContent(service)
    } else {
        AuthContentTab().getContent(service)
    }
}

fun updateToolWindowStateForAllProjects() {
    // we are using this method to sync the state of the tool window for all open projects
    // for example, after changing the auth state
    ApplicationManager.getApplication().runReadAction {
        val projects = ProjectManager.getInstance().openProjects
        projects.forEach { project ->
            updateToolWindowState(project)
        }
    }
}


fun updateToolWindowState(project: Project) {
    ApplicationManager.getApplication().invokeLater {
        WriteAction.run<Error> {
            replaceToolWindowRightPanel(project, getRightPanelDependingOnState(project))
        }
    }
}
