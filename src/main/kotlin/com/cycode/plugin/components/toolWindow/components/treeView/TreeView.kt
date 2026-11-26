package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getScanTypeDisplayName
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.treeView.components.detectionNodeContextMenu.DetectionNodeContextMenu
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.pluginLocalState
import com.cycode.plugin.services.scanResults
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.AWTEvent
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class TreeView(
    val project: Project, defaultRightPane: JComponent? = null
) : JPanel(GridLayout(1, 0)), TreeSelectionListener {
    private val tree: Tree

    // dummyRootNode is a workaround to allow us to hide the root node of the tree
    private val dummyRootNode = createNode(DummyNode())
    private val model = DefaultTreeModel(dummyRootNode)
    private val rootNodes: RootNodes = RootNodes()

    private val splitPane: JBSplitter = JBSplitter()

    private val scanResults = scanResults(project)
    private val pluginLocalState = pluginLocalState(project)

    private lateinit var treeScrollPane: JBScrollPane

    private var severityFilter: Map<String, Boolean>? = null

    // what each scan type found (e.g. "High - 2"), shown on its row while no scan of that type is running
    private val detectionSummaries = mutableMapOf<CliScanType, String>()

    init {
        createNodes(dummyRootNode)

        tree = Tree(model)
        tree.isRootVisible = false
        // lets the platform animate the spinner of a running scan in the renderer, only while one is shown
        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        tree.cellRenderer = TreeCellRenderer()

        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener(this)  // we want to listen for when the user selects a node
        tree.addMouseListener(createMouseListeners())

        val minimumSize = Dimension(400, 100)

        val treeView = JBScrollPane(tree)
        treeScrollPane = treeView
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
        // a right click selects the row too, but it only opens the context menu: open nothing before it is shown
        if (isRightClick(EventQueue.getCurrentEvent())) return

        val node = tree.getLastSelectedPathComponent() as DefaultMutableTreeNode

        openDetectionInFile(project, node.userObject as AbstractNode)
        openDetection(node.userObject as AbstractNode)
    }

    private fun isRightClick(event: AWTEvent?) =
        event is MouseEvent && (event.isPopupTrigger || SwingUtilities.isRightMouseButton(event))

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

    /** Called when a finding is picked in the tree; the owner decides what is shown for it. */
    var onDetectionSelected: ((DetectionBase) -> Unit)? = null

    fun openDetection(node: AbstractNode) {
        when (node) {
            is SecretDetectionNode -> openDetection(node.detection)
            is ScaDetectionNode -> openDetection(node.detection)
            is IacDetectionNode -> openDetection(node.detection)
            is SastDetectionNode -> openDetection(node.detection)
            else -> return
        }
    }

    fun openDetection(detection: DetectionBase) {
        onDetectionSelected?.invoke(detection)
    }

    private fun getSeverityWeight(severity: String): Int {
        return when (severity.lowercase()) {
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
            severityFilter?.getOrDefault(it.severity.lowercase(), true) != false
        }
        val detectionsByFile = filteredDetections.groupBy { it.detectionDetails.getFilepath() }

        detectionSummaries[scanType] = getDetectionSummary(filteredDetections)

        val projectRoot = project.basePath?.let { File(it) } ?: File("")

        for ((filePath, detections) in detectionsByFile) {
            val summary = CycodeBundle.message("fileNodeSummary", detections.size)
            val projectRelativePath = File(filePath).relativeTo(projectRoot).path

            val psiFile = getPsiFile(project, filePath)
            val icon = if (psiFile != null)
                psiFile.getIcon(Iconable.ICON_FLAG_READ_STATUS) else
                AllIcons.Actions.Annotate

            val fileNode = createNode(FileNode(projectRelativePath, summary, icon))

            val sortedDetections = detections.sortedWith(
                compareByDescending<DetectionBase> { getSeverityWeight(it.severity) }
                    .thenBy { it.detectionDetails.getLineNumber() }
            )

            for (detection in sortedDetections) {
                fileNode.add(createNodeCallback(detection))
            }

            rootNodes.getScanTypeNode(scanType).add(fileNode)
        }
    }

    private fun createSecretDetectionNodes() {
        val secretDetections = scanResults.secretResults
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
        val scaDetections = scanResults.scaResults
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
        val iacDetections = scanResults.iacResults
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
        val sastDetections = scanResults.sastResults
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

    private var collapsed = false

    private var proportionBeforeCollapse = 0.5f

    /** Shrinks the lower/right pane to [extent] pixels when collapsed and restores the previous split when expanded. */
    fun setSecondPaneCollapsed(collapse: Boolean, extent: Int) {
        val total = if (splitPane.orientation) splitPane.height else splitPane.width
        if (collapse) {
            if (!collapsed) proportionBeforeCollapse = splitPane.proportion
            if (total > 0) splitPane.proportion = 1f - extent.toFloat() / total
        } else if (collapsed) {
            splitPane.proportion = proportionBeforeCollapse
        }
        collapsed = collapse
    }

    fun setStacked(stacked: Boolean) {
        if (splitPane.orientation == stacked) return
        splitPane.orientation = stacked
        treeScrollPane.minimumSize = if (stacked) Dimension(100, 100) else Dimension(400, 100)
    }

    fun replaceRightPanel(newRightPanel: JComponent): TreeView {
        splitPane.secondComponent = newRightPanel
        return this
    }

    /** The rows of a scan type that is about to be scanned again start over, collapsed. */
    fun collapseScanType(scanType: CliScanType) {
        tree.collapsePath(TreePath(rootNodes.getScanTypeNode(scanType).path))
    }

    private fun updateSummaries() {
        CliScanType.entries.forEach {
            // the previous findings are cleared as soon as a new scan starts, and come back with its results
            val summary = if (pluginLocalState.isScanRunning(it)) "" else detectionSummaries[it].orEmpty()
            rootNodes.setNodeSummary(it, summary)
        }
    }

    /** Swaps the spinner icons and summaries of the scan type nodes without reloading the tree (keeps expanded nodes). */
    fun refreshRunningState() {
        CliScanType.entries.forEach { rootNodes.setRunning(it, pluginLocalState.isScanRunning(it)) }
        updateSummaries()
        CliScanType.entries.forEach { model.nodeChanged(rootNodes.getScanTypeNode(it)) }
    }

    fun refreshTree() {
        // a reload collapses every node: remember what the user expanded and restore it afterwards
        val state = TreeState.createOn(tree)

        // TODO(MarshalX): is possible to optimize this to only update the nodes that have changed
        dummyRootNode.removeAllChildren()
        createNodes(dummyRootNode)

        /*
        Never use tree.updateUI() here.
        It crashes component in 2024.1.4, maybe in other versions too.
        It did not crash in 2023.3.7 and earlier.
        */
        model.reload()

        state.applyTo(tree)
    }

    private fun createNodes(top: DefaultMutableTreeNode) {
        rootNodes.createNodes(top)
        detectionSummaries.clear()
        CliScanType.entries.forEach { rootNodes.setRunning(it, pluginLocalState.isScanRunning(it)) }
        createSecretDetectionNodes()
        createScaDetectionNodes()
        createIacDetectionNodes()
        createSastDetectionNodes()
        updateSummaries()
    }

    fun getTree() = tree

    fun updateSeverityFilter(newSeverityFilter: Map<String, Boolean>) {
        severityFilter = newSeverityFilter
        refreshTree()
    }
}
