package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import javax.swing.Icon

data class FileNode(
    override var name: String,
    override var icon: Icon? = null,
    val summary: String? = null
) : AbstractNode()
