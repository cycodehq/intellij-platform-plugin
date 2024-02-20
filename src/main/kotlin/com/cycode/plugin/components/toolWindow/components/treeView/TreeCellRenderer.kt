package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.components.toolWindow.components.treeView.nodes.AbstractNode
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

const val NODE_DELIMITER = " "
val SUMMARY_ATTRIBUTES = SimpleTextAttributes(null, JBColor.GRAY, null, SimpleTextAttributes.STYLE_PLAIN)

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

        append(nodeData.name)
        if (nodeData.summary != null) {
            append(NODE_DELIMITER)
            append(nodeData.summary!!, SUMMARY_ATTRIBUTES)
        }

        icon = nodeData.icon
        isIconOpaque = false
        isTransparentIconBackground = true

        isOpaque = false
    }
}
