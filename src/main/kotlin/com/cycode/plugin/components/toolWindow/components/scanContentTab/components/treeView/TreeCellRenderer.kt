package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView

import com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes.AbstractNode
import com.intellij.ui.ColoredTreeCellRenderer
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class TreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        if (value !is DefaultMutableTreeNode) return
        val nodeData = value.userObject as AbstractNode
        icon = nodeData.icon
        append(nodeData.name)

        isIconOpaque = false
        isTransparentIconBackground = true
        isOpaque = false
    }
}
