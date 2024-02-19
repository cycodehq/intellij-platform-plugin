package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.icons.PluginIcons
import javax.swing.tree.DefaultMutableTreeNode

class RootNodes {
    companion object {
        private val SecretsNode = createNode(
            ScanTypeNode(CycodeBundle.message("secretDisplayName"), PluginIcons.SCAN_TYPE_SECRETS)
        )

        private val SastNode = createNode(
            ScanTypeNode(CycodeBundle.message("sastDisplayName"), PluginIcons.SCAN_TYPE_SAST)
        )

        private val ScaNode = createNode(
            ScanTypeNode(CycodeBundle.message("scaDisplayName"), PluginIcons.SCAN_TYPE_SCA)
        )

        private val IacNode = createNode(
            ScanTypeNode(CycodeBundle.message("iacDisplayName"), PluginIcons.SCAN_TYPE_IAC)
        )

        private val scanTypeToNode = mapOf(
            CliScanType.Secret to SecretsNode,
            CliScanType.Sast to SastNode,
            CliScanType.Sca to ScaNode,
            CliScanType.Iac to IacNode
        )

        fun createNodes(top: DefaultMutableTreeNode) {
            SecretsNode.removeAllChildren()
            ScaNode.removeAllChildren()
            SastNode.removeAllChildren()
            IacNode.removeAllChildren()

            // the order of adding nodes is important
            top.add(SecretsNode)
            top.add(ScaNode)
            top.add(SastNode)
            top.add(IacNode)
        }

        fun getScanTypeNode(scanType: CliScanType): DefaultMutableTreeNode {
            return scanTypeToNode[scanType] ?: throw IllegalArgumentException("Unknown scan type: $scanType")
        }
    }
}
