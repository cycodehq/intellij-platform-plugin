package com.cycode.plugin.components.toolWindow.components.treeView.nodes

import javax.swing.tree.DefaultMutableTreeNode

fun createNode(node: AbstractNode): DefaultMutableTreeNode {
    return DefaultMutableTreeNode(node)
}
