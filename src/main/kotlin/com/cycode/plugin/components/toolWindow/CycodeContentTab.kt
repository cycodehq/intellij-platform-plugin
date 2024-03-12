package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.components.toolWindow.components.treeView.TreeView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import javax.swing.JPanel

class CycodeContentTab(project: Project) : SimpleToolWindowPanel(false, false) {
    private val treeView = TreeView(project)

    init {
        setContent(treeView)
    }

    fun updateContent(rightPanel: JPanel): JPanel {
        treeView.refreshTree()
        return treeView.replaceRightPanel(rightPanel)
    }

    fun getTreeView() = treeView
}
