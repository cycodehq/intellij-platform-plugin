package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.icons.PluginIcons
import javax.swing.tree.DefaultMutableTreeNode

class RootNodes {
    private val secretNode = createNode(
        ScanTypeNode(
            CycodeBundle.message("secretDisplayName"),
            CycodeBundle.message("secretNodeSummary"),
            PluginIcons.SCAN_TYPE_SECRETS
        )
    )

    private val scaNode = createNode(
        ScanTypeNode(
            CycodeBundle.message("scaDisplayName"),
            CycodeBundle.message("scaNodeSummary"),
            PluginIcons.SCAN_TYPE_SCA
        )
    )

    private val sastNode = createNode(
        ScanTypeNode(
            CycodeBundle.message("sastDisplayName"),
            CycodeBundle.message("sastNodeSummary"),
            PluginIcons.SCAN_TYPE_SAST
        )
    )

    private val iacNode = createNode(
        ScanTypeNode(
            CycodeBundle.message("iacDisplayName"),
            CycodeBundle.message("iacNodeSummary"),
            PluginIcons.SCAN_TYPE_IAC
        )
    )

    private val scanTypeToNode = mapOf(
        CliScanType.Secret to secretNode,
        CliScanType.Sast to sastNode,
        CliScanType.Sca to scaNode,
        CliScanType.Iac to iacNode
    )

    fun setNodeSummary(scanType: CliScanType, summary: String) {
        val node = getScanTypeNode(scanType)
        (node.userObject as ScanTypeNode).summary = summary
    }

    fun createNodes(top: DefaultMutableTreeNode) {
        secretNode.removeAllChildren()
        scaNode.removeAllChildren()
        sastNode.removeAllChildren()
        iacNode.removeAllChildren()

        // the order of adding nodes is important
        top.add(secretNode)
        top.add(scaNode)
        top.add(sastNode)
        top.add(iacNode)
    }

    fun getScanTypeNode(scanType: CliScanType): DefaultMutableTreeNode {
        return scanTypeToNode[scanType] ?: throw IllegalArgumentException("Unknown scan type: $scanType")
    }
}
