package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView.nodes.*
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.scanResults
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel


class TreeView(val project: Project, defaultRightPane: Component) : JPanel(GridLayout(1, 0)), TreeSelectionListener {
    private val tree: Tree
    private val scanResults = scanResults(project)

    init {
        val top = createNode(DummyNode())
        createNodes(top)

        tree = Tree(top)
        tree.setRootVisible(false)
        tree.setCellRenderer(TreeCellRenderer())

        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener(this)  // we want to listen for when the user selects a node

        val treeView = JBScrollPane(tree)

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        flattenJSplitPane(splitPane)

        splitPane.leftComponent = treeView
        splitPane.rightComponent = defaultRightPane

        val minimumSize = Dimension(400, 100)
        defaultRightPane.minimumSize = minimumSize
        treeView.minimumSize = minimumSize

        splitPane.resizeWeight = 0.5

        add(splitPane)
    }

    override fun valueChanged(e: TreeSelectionEvent) {
        if (tree.getLastSelectedPathComponent() == null) return

        val node = tree.getLastSelectedPathComponent() as DefaultMutableTreeNode

        if (node.userObject is SecretDetectionNode) {
            openSecretDetectionInFile(node.userObject as SecretDetectionNode)
        }

        if (node.userObject is ScaDetectionNode) {
            openScaDetectionInFile(node.userObject as ScaDetectionNode)
            displayScaViolationCard(node.userObject as ScaDetectionNode)
        }
    }

    private fun openSecretDetectionInFile(node: SecretDetectionNode) {
        val filePath = node.detection.detectionDetails.getFilepath()
        val line = node.detection.detectionDetails.line
        openFileInEditor(project, filePath, line)
    }

    private fun openScaDetectionInFile(node: ScaDetectionNode) {
        val filePath = node.detection.detectionDetails.getFilepath()
        val line = node.detection.detectionDetails.lineInFile - 1
        openFileInEditor(project, filePath, line)
    }

    private fun displayScaViolationCard(node: ScaDetectionNode) {
        // not implemented yet
    }

    private fun convertSeverityToIcon(severity: String): Icon {
        return when (severity.toLowerCase()) {
            "critical" -> PluginIcons.SEVERITY_CRITICAL
            "high" -> PluginIcons.SEVERITY_HIGH
            "medium" -> PluginIcons.SEVERITY_MEDIUM
            "low" -> PluginIcons.SEVERITY_LOW
            else -> PluginIcons.SEVERITY_INFO
        }
    }

    private fun getSeverityWeight(severity: String): Int {
        return when (severity.toLowerCase()) {
            "critical" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 0
        }
    }

    private fun createDetectionNodes(
        scanType: CliScanType,
        scanResults: ScanResultBase,
        createNodeCallback: (detection: DetectionBase) -> DefaultMutableTreeNode
    ) {
        val sortedDetections = scanResults.detections.sortedByDescending { getSeverityWeight(it.severity) }
        val detectionsByFile = sortedDetections.groupBy { it.detectionDetails.getFilepath() }

        for ((filePath, detections) in detectionsByFile) {
            val fileName = File(filePath).name
            val fileNode = createNode(FileNode(fileName))
            for (detection in detections) {
                fileNode.add(createNodeCallback(detection))
            }
            RootNodes.getScanTypeNode(scanType).add(fileNode)
        }
    }

    private fun createSecretDetectionNodes() {
        val secretDetections = scanResults.getSecretResults()
        if (secretDetections !is CliResult.Success) {
            return
        }

        fun createSecretDetectionNode(detection: DetectionBase): DefaultMutableTreeNode {
            return createNode(
                SecretDetectionNode(
                    detection.getFormattedNodeTitle(),
                    convertSeverityToIcon(detection.severity),
                    detection as SecretDetection
                )
            )
        }

        createDetectionNodes(CliScanType.Secret, secretDetections.result, ::createSecretDetectionNode)
    }

    private fun createScaDetectionNodes() {
        val scaDetections = scanResults.getScaResults()
        if (scaDetections !is CliResult.Success) {
            return
        }

        fun createScaDetectionNode(detection: DetectionBase): DefaultMutableTreeNode {
            return createNode(
                ScaDetectionNode(
                    detection.getFormattedNodeTitle(),
                    convertSeverityToIcon(detection.severity),
                    detection as ScaDetection
                )
            )
        }

        createDetectionNodes(CliScanType.Sca, scaDetections.result, ::createScaDetectionNode)
    }

    private fun createNodes(top: DefaultMutableTreeNode) {
        RootNodes.createNodes(top)
        createSecretDetectionNodes()
        createScaDetectionNodes()
    }
}
