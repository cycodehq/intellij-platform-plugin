package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.CycodeActionToolbar
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.cycode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent

class CycodeToolWindowFactory : DumbAware, ToolWindowFactory {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(PluginIcons.TOOL_WINDOW)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = cycode(project)

        val contentTab = CycodeContentTab(project)
        TabManager.addTab(project, contentTab)

        CycodeActionToolbar().attachActionToolbar(contentTab)

        contentTab.showStatePanel()

        val toolWindowContent = createToolWindowContent(contentTab)
        toolWindow.contentManager.addContent(toolWindowContent)

        Disposer.register(service, toolWindowContent)

        // re-layout the scan tab when the tool window is moved to another side
        var lastAnchor = toolWindow.anchor
        project.messageBus.connect(service).subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(
                toolWindowManager: ToolWindowManager,
                changedToolWindow: ToolWindow,
                changeType: ToolWindowManagerEventType
            ) {
                if (changeType != ToolWindowManagerEventType.MovedOrResized) return
                val anchor = toolWindowManager.getToolWindow(CycodeBundle.message("toolWindowId"))?.anchor ?: return
                if (anchor != lastAnchor) {
                    lastAnchor = anchor
                    updateToolWindowState(project, reloadResults = false)
                }
            }
        })
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

private fun createToolWindowContent(component: JComponent): Content {
    return ContentFactory.getInstance().createContent(component, null, false)
}

fun updateToolWindowStateForAllProjects() {
    // we are using this method to sync the state of the tool window for all open projects,
    // for example, after changing the auth state
    ApplicationManager.getApplication().runReadAction {
        val projects = ProjectManager.getInstance().openProjects
        projects.forEach { project ->
            updateToolWindowState(project)
        }
    }
}


/**
 * Full rebuild: replaces the right panel for the current state. [reloadResults] = false keeps the results tree
 * (and its expanded nodes), for changes that only affect the layout (scope, collapse, docking).
 */
fun updateToolWindowState(project: Project, reloadResults: Boolean = true) {
    ApplicationManager.getApplication().invokeLater {
        val contentTab = CycodeToolWindowFactory.TabManager.getTab(project) ?: return@invokeLater
        WriteAction.run<Error> { contentTab.showStatePanel(reloadResults) }
    }
}

/** A scan started or finished: only the scan buttons and the tree spinners change, nothing is rebuilt. */
fun refreshRunningState(project: Project) {
    ApplicationManager.getApplication().invokeLater {
        CycodeToolWindowFactory.TabManager.getTab(project)?.refreshRunningState()
    }
}

fun activateToolWindow(project: Project) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val toolWindow = toolWindowManager.getToolWindow(CycodeBundle.message("toolWindowId"))
    toolWindow?.activate(null)
}
