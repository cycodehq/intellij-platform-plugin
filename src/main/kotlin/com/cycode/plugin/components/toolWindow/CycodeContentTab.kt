package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.components.treeView.TreeView
import com.cycode.plugin.services.pluginLocalState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ui.JBUI
import javax.swing.JPanel

class CycodeContentTab(private val project: Project) : SimpleToolWindowPanel(false, false) {
    private val treeView = TreeView(project)

    init {
        setContent(treeView)
        treeView.collapsedExtent = ::getCollapsedExtent
    }

    // left/right docking is narrow and tall: results above the scan controls; bottom: side by side
    private fun isStacked(): Boolean {
        val anchor = ToolWindowManager.getInstance(project).getToolWindow(CycodeBundle.message("toolWindowId"))?.anchor
        return anchor != null && !anchor.isHorizontal
    }

    private fun getCollapsedExtent() = JBUI.scale(if (isStacked()) COLLAPSED_HEIGHT else COLLAPSED_WIDTH)

    fun updateContent(rightPanel: JPanel): JPanel {
        treeView.setStacked(isStacked())
        treeView.setSecondPaneCollapsed(pluginLocalState(project).scanControlsCollapsed, getCollapsedExtent())
        treeView.refreshTree()
        return treeView.replaceRightPanel(rightPanel)
    }

    fun getTreeView() = treeView

    companion object {
        private const val COLLAPSED_HEIGHT = 30
        private const val COLLAPSED_WIDTH = 180
    }
}
