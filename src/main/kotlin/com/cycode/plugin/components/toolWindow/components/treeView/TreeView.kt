package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.ScaViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.scanContentTab.ScanContentTab
import com.cycode.plugin.components.toolWindow.components.treeView.components.detectionNodeContextMenu.DetectionNodeContextMenu
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.cycode
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
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class TreeView(
    val project: Project, defaultRightPane: JComponent? = null
) : JPanel(GridLayout(1, 0)), TreeSelectionListener {
    private val tree: Tree
    private val service = cycode(project)

    // dummyRootNode is a workaround to allow us to hide the root node of the tree
    private val dummyRootNode = createNode(DummyNode())
    private val rootNodes: RootNodes = RootNodes()

    private val splitPane: JBSplitter = JBSplitter()

    private val scanResults = scanResults(project)

    private var severityFilter: Map<String, Boolean>? = null

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

        if (defaultRightPane != null) {
            splitPane.secondComponent = defaultRightPane
            defaultRightPane.minimumSize = minimumSize
        }

        add(splitPane)
    }

    override fun valueChanged(e: TreeSelectionEvent) {
        if (tree.getLastSelectedPathComponent() == null) return

        val node = tree.getLastSelectedPathComponent() as DefaultMutableTreeNode

        when (node.userObject) {
            is SecretDetectionNode -> {
                openSecretDetectionInFile(project, node.userObject as SecretDetectionNode)
                displaySecretViolationCard(node.userObject as SecretDetectionNode)
            }

            is ScaDetectionNode -> {
                openScaDetectionInFile(project, node.userObject as ScaDetectionNode)
                displayScaViolationCard(node.userObject as ScaDetectionNode)
            }

            is IacDetectionNode -> {
                openIacDetectionInFile(project, node.userObject as IacDetectionNode)
                displayIacViolationCard(node.userObject as IacDetectionNode)
            }
        }
    }

    private fun createMouseListeners(): MouseAdapter {
        val treeView = this

        return object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val selRow: Int = tree.getRowForLocation(e.x, e.y)
                val selPath: TreePath? = tree.getPathForLocation(e.x, e.y)
                if (selRow != -1 && selPath != null) {
                    // single right mouse click
                    if (e.button == MouseEvent.BUTTON3 && e.clickCount == 1) {
                        DetectionNodeContextMenu(treeView, project, selPath).showPopup(e)
                    }
                }
            }
        }
    }

    fun displaySecretViolationCard(node: SecretDetectionNode) {
        // we don't have a dedicated card yet for secret violations,
        // so we are returning to the main content tab
        replaceRightPanel(ScanContentTab().getContent(service))
    }

    fun displayIacViolationCard(node: IacDetectionNode) {
        // we don't have a dedicated card yet for IaC violations,
        // so we are returning to the main content tab
        replaceRightPanel(ScanContentTab().getContent(service))
    }

    fun displayScaViolationCard(node: ScaDetectionNode) {
        replaceRightPanel(ScaViolationCardContentTab().getContent(node.detection))
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
        val filteredDetections = scanResults.detections.filter {
            severityFilter?.getOrDefault(it.severity.toLowerCase(), true) ?: true
        }
        val sortedDetections = filteredDetections.sortedByDescending { getSeverityWeight(it.severity) }
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
                    PluginIcons.getSeverityIcon(detection.severity),
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
                    PluginIcons.getSeverityIcon(detection.severity),
                    detection as ScaDetection
                )
            )
        }

        createDetectionNodes(CliScanType.Sca, scaDetections.result, ::createScaDetectionNode)
    }

    private fun createIacDetectionNodes() {
        val iacDetections = scanResults.getIacResults()
        if (iacDetections !is CliResult.Success) {
            return
        }

        fun createIacDetectionNode(detection: DetectionBase): DefaultMutableTreeNode {
            return createNode(
                IacDetectionNode(
                    detection.getFormattedNodeTitle(),
                    PluginIcons.getSeverityIcon(detection.severity),
                    detection as IacDetection
                )
            )
        }

        createDetectionNodes(CliScanType.Iac, iacDetections.result, ::createIacDetectionNode)
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
        createIacDetectionNodes()
    }

    fun getTree() = tree

    fun updateSeverityFilter(newSeverityFilter: Map<String, Boolean>) {
        severityFilter = newSeverityFilter
        refreshTree()
    }
}
