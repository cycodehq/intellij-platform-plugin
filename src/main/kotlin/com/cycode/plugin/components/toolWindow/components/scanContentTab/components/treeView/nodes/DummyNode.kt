package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import com.cycode.plugin.CycodeBundle
import javax.swing.Icon

data class DummyNode(
    override var name: String = CycodeBundle.message("name"),
    override var icon: Icon? = null,
    val summary: String? = null
) : AbstractNode()
