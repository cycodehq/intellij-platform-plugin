package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.components.toolWindow.components.treeView.TreeView
import com.cycode.plugin.CycodeBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.cycode.plugin.services.pluginLocalState
import com.intellij.util.ui.JBUI
import javax.swing.JPanel

class CycodeContentTab(private val project: Project) : SimpleToolWindowPanel(false, false) {
    private val treeView = TreeView(project)

    init {
        setContent(treeView)
    }

    fun updateContent(rightPanel: JPanel): JPanel {
        // left/right docking is narrow and tall: results above the scan controls; bottom: side by side
        val anchor = ToolWindowManager.getInstance(project).getToolWindow(CycodeBundle.message("toolWindowId"))?.anchor
        treeView.setStacked(anchor != null && !anchor.isHorizontal)
        treeView.setSecondPaneCollapsed(
            pluginLocalState(project).scanControlsCollapsed,
            if (anchor != null && !anchor.isHorizontal) JBUI.scale(COLLAPSED_HEIGHT) else JBUI.scale(COLLAPSED_WIDTH)
        )
        treeView.refreshTree()
        return treeView.replaceRightPanel(rightPanel)
    }

    fun getTreeView() = treeView

    companion object {
        private const val COLLAPSED_HEIGHT = 30
        private const val COLLAPSED_WIDTH = 180
    }
}
