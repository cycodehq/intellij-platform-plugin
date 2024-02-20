package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.components.toolWindow.components.treeView.TreeView
import com.intellij.openapi.project.Project
import javax.swing.JPanel

class CycodeContentTab(project: Project) {
    private val treeView = TreeView(project)

    fun updateContent(rightPanel: JPanel): JPanel {
        treeView.refreshTree()
        return treeView.replaceRightPanel(rightPanel)
    }
}
