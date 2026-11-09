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
                    updateToolWindowState(project, ToolWindowUpdate.LayoutChanged)
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

/** The scan tab is the panel currently on screen (a finding, the auth page etc. may be shown instead). */
private fun getShownScanTab(project: Project): ScanContentTab? {
    val local = pluginLocalState(project)
    val scanTab = ScanTabs.get(project) ?: return null
    val contentTab = CycodeToolWindowFactory.TabManager.getTab(project) ?: return null
    val shown = local.cliInstalled && local.cliAuthed && contentTab.getTreeView().getRightPanel() === scanTab.panel
    return scanTab.takeIf { shown }
}

/**
 * Updates the tool window as little as the [update] allows: the visible scan tab is refreshed in place when possible,
 * to avoid flicker, and the results tree is only reloaded when the results may have changed.
 */
fun updateToolWindowState(project: Project, update: ToolWindowUpdate = ToolWindowUpdate.StateChanged) {
    ApplicationManager.getApplication().invokeLater {
        val treeView = CycodeToolWindowFactory.TabManager.getTab(project)?.getTreeView()
        val scanTab = getShownScanTab(project)

        var action = resolveToolWindowAction(update, scanTab != null)
        // something structural changed (e.g. the scope): the shown scan tab cannot be refreshed in place
        if (action is ToolWindowAction.RefreshScanTab && scanTab?.refresh(cycode(project)) != true) {
            action = resolveToolWindowAction(update, scanTabShown = false)
        }

        when (action) {
            is ToolWindowAction.RefreshScanTab ->
                if (action.reloadResults) treeView?.refreshTree() else treeView?.refreshRunningState()

            ToolWindowAction.RefreshSpinners -> treeView?.refreshRunningState()
            is ToolWindowAction.Rebuild -> WriteAction.run<Error> {
                replaceToolWindowRightPanel(project, getRightPanelDependingOnState(project), action.reloadResults)
            }
        }
    }
}

fun activateToolWindow(project: Project) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val toolWindow = toolWindowManager.getToolWindow(CycodeBundle.message("toolWindowId"))
    toolWindow?.activate(null)
}
