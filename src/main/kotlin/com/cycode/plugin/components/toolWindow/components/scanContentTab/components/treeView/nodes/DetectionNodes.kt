package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import javax.swing.Icon

data class SecretDetectionNode(
    override var name: String,
    override var icon: Icon?,
    val detection: SecretDetection,
    override var summary: String? = null,
) : AbstractNode()

data class ScaDetectionNode(
    override var name: String,
    override var icon: Icon?,
    val detection: ScaDetection,
    override var summary: String? = null,
) : AbstractNode()
