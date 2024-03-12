package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.components.toolWindow.CycodeContentTab
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.AnAction

class CollapseAllAction {
    companion object {
        fun create(contentTab: CycodeContentTab): AnAction {
            val tree = contentTab.getTreeView().getTree()
            val treeExpander = DefaultTreeExpander(tree)

            val commonActionsManager = CommonActionsManager.getInstance()
            return commonActionsManager.createCollapseAllAction(treeExpander, tree)
        }
    }
}
