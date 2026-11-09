package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.getRightPanelDependingOnState
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getScanTypeDisplayName
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.createViolationCard
import com.cycode.plugin.editor.FindingTabService
import com.cycode.plugin.services.pluginSettings
import com.intellij.openapi.components.service
import com.cycode.plugin.components.toolWindow.components.treeView.components.detectionNodeContextMenu.DetectionNodeContextMenu
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.pluginLocalState
import com.cycode.plugin.services.scanResults
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.ui.InplaceButton
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
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
        when (node) {
            is SecretDetectionNode -> displayViolationCard(node.detection)
            is ScaDetectionNode -> displayViolationCard(node.detection)
            is IacDetectionNode -> displayViolationCard(node.detection)
            is SastDetectionNode -> displayViolationCard(node.detection)
            else -> return
        }
    }

    fun displayViolationCard(detection: DetectionBase) {
        val card = createViolationCard(project, detection) ?: return

        // with the option on, vulnerabilities get an editor tab on the right instead of the tool window pane
        if (pluginSettings().openFindingsInEditorTab) {
            project.service<FindingTabService>().show(card)
            return
        }

        // a violation card needs the room: the scan options expand for it and collapse again when it is closed
        val cardAlreadyShown = splitPane.secondComponent?.getClientProperty(VIOLATION_CARD_KEY) == true
        if (pluginLocalState.scanOptionsCollapse.onCardOpened(cardAlreadyShown)) {
            setSecondPaneCollapsed(false, 0)
        }
        replaceRightPanel(wrapWithCloseButton(card))
    }

    /** Adds a close button above a violation card that returns to the scan view. */
    private fun wrapWithCloseButton(card: JComponent): JPanel {
        val closeButton = InplaceButton(CycodeBundle.message("violationCardCloseTooltip"), AllIcons.Actions.Close) {
            tree.clearSelection()
            if (pluginLocalState.scanOptionsCollapse.onCardClosed()) {
                setSecondPaneCollapsed(true, collapsedExtent())
            }
            replaceRightPanel(getRightPanelDependingOnState(project))
        }

        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8)
            add(closeButton, BorderLayout.EAST)
        }

        return JPanel(BorderLayout()).apply {
            putClientProperty(VIOLATION_CARD_KEY, true)
            add(header, BorderLayout.NORTH)
            add(card, BorderLayout.CENTER)
        }
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

        rootNodes.setNodeSummary(scanType, getDetectionSummary(filteredDetections))

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

    /** Size of the collapsed scan options; set by the owner because it depends on the docking. */
    var collapsedExtent: () -> Int = { 0 }
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

    fun getRightPanel(): JComponent? = splitPane.secondComponent

    fun replaceRightPanel(newRightPanel: JComponent): TreeView {
        splitPane.secondComponent = newRightPanel
        return this
    }

    /** Swaps the spinner icons of the scan type nodes without reloading the tree (keeps expanded nodes). */
    fun refreshRunningState() {
        CliScanType.entries.forEach {
            rootNodes.setRunning(it, pluginLocalState.isScanRunning(it))
            model.nodeChanged(rootNodes.getScanTypeNode(it))
        }
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
        // the rows of a scan type that is running again start over (this also covers the Scan all buttons)
        CliScanType.entries.filter { pluginLocalState.isScanRunning(it) }.forEach {
            tree.collapsePath(TreePath(rootNodes.getScanTypeNode(it).path))
        }
    }

    private fun createNodes(top: DefaultMutableTreeNode) {
        rootNodes.createNodes(top)
        CliScanType.entries.forEach { rootNodes.setRunning(it, pluginLocalState.isScanRunning(it)) }
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

    companion object {
        private const val VIOLATION_CARD_KEY = "cycode.violationCard"
    }
}
