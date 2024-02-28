package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.scanResults
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

const val DIFFERENCE_BETWEEN_SCA_LINE_NUMBERS = 1

class TreeView(
    val project: Project, defaultRightPane: JComponent? = null
) : JPanel(GridLayout(1, 0)), TreeSelectionListener {
    private val tree: Tree

    // dummyRootNode is a workaround to allow us to hide the root node of the tree
    private val dummyRootNode = createNode(DummyNode())
    private val rootNodes: RootNodes = RootNodes()

    private val splitPane: JBSplitter = JBSplitter()

    private val scanResults = scanResults(project)

    init {
        createNodes(dummyRootNode)

        tree = Tree(dummyRootNode)
        tree.setRootVisible(false)
        tree.setCellRenderer(TreeCellRenderer())

        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener(this)  // we want to listen for when the user selects a node
        tree.addMouseListener(createMouseListeners())

        val minimumSize = Dimension(400, 100)

        val treeView = JBScrollPane(tree)
        treeView.border = SideBorder(JBColor.GRAY, SideBorder.RIGHT)
        treeView.minimumSize = minimumSize

        splitPane.firstComponent = treeView
        splitPane.isShowDividerControls = true
        splitPane.isShowDividerIcon = true

        if (defaultRightPane != null) {
            splitPane.secondComponent = defaultRightPane
            defaultRightPane.minimumSize = minimumSize
        }

        add(splitPane)
    }

    override fun valueChanged(e: TreeSelectionEvent) {
        if (tree.getLastSelectedPathComponent() == null) return

        val node = tree.getLastSelectedPathComponent() as DefaultMutableTreeNode

        if (node.userObject is SecretDetectionNode) {
            openSecretDetectionInFile(project, node.userObject as SecretDetectionNode)
        }

        if (node.userObject is ScaDetectionNode) {
            openScaDetectionInFile(project, node.userObject as ScaDetectionNode)
            displayScaViolationCard(node.userObject as ScaDetectionNode)
        }
    }

    private fun createMouseListeners(): MouseAdapter {
        return object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val selRow: Int = tree.getRowForLocation(e.x, e.y)
                val selPath: TreePath? = tree.getPathForLocation(e.x, e.y)
                if (selRow != -1 && selPath != null) {
                    // single right mouse click
                    if (e.button == MouseEvent.BUTTON3 && e.clickCount == 1) {
                        DetectionNodeContextMenu(project, selPath).showPopup(e)
                    }
                }
            }
        }
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

    private fun getDetectionSummary(sortedDetections: List<DetectionBase>): String {
        // detections must be sorted by severity
        return sortedDetections.groupBy { it.severity }
            .map { (severity, detections) -> "$severity - ${detections.size}" }
            .joinToString(" | ")
    }

    private fun createDetectionNodes(
        scanType: CliScanType,
        scanResults: ScanResultBase,
        createNodeCallback: (detection: DetectionBase) -> DefaultMutableTreeNode
    ) {
        val sortedDetections = scanResults.detections.sortedByDescending { getSeverityWeight(it.severity) }
        val detectionsByFile = sortedDetections.groupBy { it.detectionDetails.getFilepath() }

        rootNodes.setNodeSummary(scanType, getDetectionSummary(sortedDetections))

        for ((filePath, detections) in detectionsByFile) {
            val fileName = File(filePath).name
            val summary = CycodeBundle.message("fileNodeSummary", detections.size)

            val fileNode = createNode(FileNode(fileName, summary))
            for (detection in detections) {
                fileNode.add(createNodeCallback(detection))
            }

            rootNodes.getScanTypeNode(scanType).add(fileNode)
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

    fun replaceRightPanel(newRightPanel: JComponent): TreeView {
        splitPane.secondComponent = newRightPanel
        return this
    }

    fun refreshTree() {
        // TODO(MarshalX): is possible to optimize this to only update the nodes that have changed
        dummyRootNode.removeAllChildren()
        createNodes(dummyRootNode)
        tree.updateUI()
    }

    private fun createNodes(top: DefaultMutableTreeNode) {
        rootNodes.createNodes(top)
        createSecretDetectionNodes()
        createScaDetectionNodes()
    }
}
