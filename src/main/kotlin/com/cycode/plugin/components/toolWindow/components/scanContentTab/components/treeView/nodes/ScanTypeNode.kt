package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import javax.swing.Icon

data class ScanTypeNode(
    override var name: String,
    override var icon: Icon?,
    val summary: String? = null
) : AbstractNode()
