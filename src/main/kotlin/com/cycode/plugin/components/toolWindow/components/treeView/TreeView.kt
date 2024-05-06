package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.treeView.components.detectionNodeContextMenu.DetectionNodeContextMenu
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.IacViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.SastViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.ScaViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.SecretViolationCardContentTab
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.scanResults
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
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

        openDetectionInFile(project, node.userObject as AbstractNode)
        displayViolationCard(node.userObject as AbstractNode)
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

    fun displayViolationCard(node: AbstractNode) {
        val card = when (node) {
            is SecretDetectionNode -> SecretViolationCardContentTab().getContent(node.detection)
            is ScaDetectionNode -> ScaViolationCardContentTab().getContent(node.detection)
            is IacDetectionNode -> IacViolationCardContentTab().getContent(node.detection)
            is SastDetectionNode -> SastViolationCardContentTab().getContent(node.detection)
            else -> return
        }

        replaceRightPanel(card)
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

        val projectRoot = project.basePath?.let { File(it) } ?: File("")

        for ((filePath, detections) in detectionsByFile) {
            val summary = CycodeBundle.message("fileNodeSummary", detections.size)
            val projectRelativePath = File(filePath).relativeTo(projectRoot).path

            val psiFile = getPsiFile(project, filePath)
            val icon = if (psiFile != null)
                psiFile.getIcon(Iconable.ICON_FLAG_READ_STATUS) else
                AllIcons.Actions.Annotate

            val fileNode = createNode(FileNode(projectRelativePath, summary, icon))
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

    private fun createSastDetectionNodes() {
        val sastDetections = scanResults.getSastResults()
        if (sastDetections !is CliResult.Success) {
            return
        }

        fun createSastDetectionNode(detection: DetectionBase): DefaultMutableTreeNode {
            return createNode(
                SastDetectionNode(
                    detection.getFormattedNodeTitle(),
                    PluginIcons.getSeverityIcon(detection.severity),
                    detection as SastDetection
                )
            )
        }

        createDetectionNodes(CliScanType.Sast, sastDetections.result, ::createSastDetectionNode)
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
        createSastDetectionNodes()
    }

    fun getTree() = tree

    fun updateSeverityFilter(newSeverityFilter: Map<String, Boolean>) {
        severityFilter = newSeverityFilter
        refreshTree()
    }
}
