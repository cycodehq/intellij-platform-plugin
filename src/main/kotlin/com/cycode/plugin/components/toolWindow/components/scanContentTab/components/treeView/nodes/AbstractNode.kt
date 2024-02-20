package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import javax.swing.Icon


abstract class AbstractNode {
    abstract var name: String
    abstract var summary: String?
    abstract var icon: Icon?

    override fun toString(): String {
        return name
    }
}
