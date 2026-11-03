package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.components.authContentTab.AuthContentTab
import com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.CycodeActionToolbar
import com.cycode.plugin.components.toolWindow.components.loadingContentTab.LoadingContentTab
import com.cycode.plugin.components.toolWindow.components.scanContentTab.ScanContentTab
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.pluginLocalState
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
import java.util.concurrent.ConcurrentHashMap
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
                    updateToolWindowState(project, forceRebuild = true, reloadResults = false)
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

private fun replaceToolWindowRightPanel(project: Project, panel: JPanel, reloadResults: Boolean) {
    val contentTab = CycodeToolWindowFactory.TabManager.getTab(project) ?: return
    contentTab.updateContent(panel, reloadResults)
}

private fun createToolWindowContent(component: JPanel): Content {
    return ContentFactory.getInstance().createContent(component, null, false)
}

fun getRightPanelDependingOnState(project: Project): JPanel {
    val service = cycode(project)
    val pluginLocalState = pluginLocalState(project)

    if (!pluginLocalState.cliInstalled) {
        return LoadingContentTab().getContent(service)
    }

    return if (pluginLocalState.cliAuthed) {
        val scanTab = ScanContentTab()
        ScanTabs.add(project, scanTab)
        scanTab.getContent(service)
    } else {
        AuthContentTab().getContent(service)
    }
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


object ScanTabs {
    private val tabs = ConcurrentHashMap<Project, ScanContentTab>()

    fun add(project: Project, tab: ScanContentTab) {
        tabs[project] = tab
    }

    fun get(project: Project): ScanContentTab? = tabs[project]

    fun remove(project: Project) {
        tabs.remove(project)
    }
}

/** Updates the scan buttons of the visible scan tab without replacing the panel; false if a rebuild is needed. */
private fun refreshScanTabInPlace(project: Project, reloadResults: Boolean): Boolean {
    val local = pluginLocalState(project)
    val scanTab = ScanTabs.get(project) ?: return false
    val contentTab = CycodeToolWindowFactory.TabManager.getTab(project) ?: return false
    if (!local.cliInstalled || !local.cliAuthed || contentTab.getTreeView().getRightPanel() !== scanTab.panel) {
        return false
    }
    if (!scanTab.refresh(cycode(project))) {
        return false
    }

    if (reloadResults) {
        contentTab.getTreeView().refreshTree()
    } else {
        contentTab.getTreeView().refreshRunningState()
    }
    return true
}

/**
 * [forceRebuild] replaces the right panel even when the scan tab is already shown; use it when the layout changes
 * (scope, collapse, docking). Otherwise the visible scan tab is only refreshed in place, to avoid flicker.
 * [reloadResults] reloads the results tree; pass false when the results did not change (a scan started or
 * finished, or only the layout changed), so the user's expanded nodes are kept.
 */
fun updateToolWindowState(project: Project, forceRebuild: Boolean = false, reloadResults: Boolean = true) {
    ApplicationManager.getApplication().invokeLater {
        if (!forceRebuild) {
            if (refreshScanTabInPlace(project, reloadResults)) {
                return@invokeLater
            }

            if (!reloadResults) {
                // only the running state changed: do not replace whatever else is shown (e.g. an open finding)
                CycodeToolWindowFactory.TabManager.getTab(project)?.getTreeView()?.refreshRunningState()
                return@invokeLater
            }
        }

        WriteAction.run<Error> {
            replaceToolWindowRightPanel(project, getRightPanelDependingOnState(project), reloadResults)
        }
    }
}

fun activateToolWindow(project: Project) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val toolWindow = toolWindowManager.getToolWindow(CycodeBundle.message("toolWindowId"))
    toolWindow?.activate(null)
}
