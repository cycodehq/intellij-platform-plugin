package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.authContentTab.AuthContentTab
import com.cycode.plugin.components.toolWindow.components.loadingContentTab.LoadingContentTab
import com.cycode.plugin.components.toolWindow.components.scanContentTab.ScanContentTab
import com.cycode.plugin.components.toolWindow.components.treeView.TreeView
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.IacViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.SastViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.ScaViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.SecretViolationCardContentTab
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.pluginLocalState
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.InplaceButton
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Owns the tool window content: the results tree and the panel next to it. The tree only reports which finding the
 * user picked; this class decides what is shown for it and how the layout adapts.
 */
class CycodeContentTab(private val project: Project) : SimpleToolWindowPanel(false, false) {
    private val treeView = TreeView(project)

    // the scan tab while it is the panel on screen (the loading, auth and finding panels replace it)
    private var scanTab: ScanContentTab? = null

    init {
        setContent(treeView)
        treeView.onDetectionSelected = ::showDetection
    }

    // left/right docking is narrow and tall: results above the scan controls; bottom: side by side
    private fun isStacked(): Boolean {
        val anchor = ToolWindowManager.getInstance(project).getToolWindow(CycodeBundle.message("toolWindowId"))?.anchor
        return anchor != null && !anchor.isHorizontal
    }

    private fun getCollapsedExtent() = JBUI.scale(if (isStacked()) COLLAPSED_HEIGHT else COLLAPSED_WIDTH)

    /**
     * Replaces the right panel with the one for the current plugin state (loading, authentication or scan).
     * [reloadResults] = false keeps the results tree (and its expanded nodes) untouched, for layout-only changes.
     */
    fun showStatePanel(reloadResults: Boolean = true) {
        val service = cycode(project)
        val local = pluginLocalState(project)

        scanTab = null
        val panel = when {
            !local.cliInstalled -> LoadingContentTab().getContent(service)
            local.cliAuthed -> ScanContentTab().also { scanTab = it }.getContent(service)
            else -> AuthContentTab().getContent(service)
        }
        show(panel, reloadResults)
    }

    /** Only the scan buttons and the spinners in the tree change; whatever is shown is never replaced. */
    fun refreshRunningState() {
        scanTab?.takeIf { it.panel === treeView.getRightPanel() }?.refreshRunningState(project)
        treeView.refreshRunningState()
    }

    /** Shows a finding in the right panel, with a close button that returns to the scan view. */
    fun showDetection(detection: DetectionBase) {
        val card = when (detection) {
            is SecretDetection -> SecretViolationCardContentTab(project).getContent(detection)
            is ScaDetection -> ScaViolationCardContentTab(project).getContent(detection)
            is IacDetection -> IacViolationCardContentTab(project).getContent(detection)
            is SastDetection -> SastViolationCardContentTab(project).getContent(detection)
            else -> return
        }

        scanTab = null
        show(wrapWithCloseButton(card), reloadResults = false)
    }

    private fun wrapWithCloseButton(card: JComponent): JPanel {
        val closeButton = InplaceButton(CycodeBundle.message("violationCardCloseTooltip"), AllIcons.Actions.Close) {
            treeView.getTree().clearSelection()
            showStatePanel(reloadResults = false)
        }

        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8)
            add(closeButton, BorderLayout.EAST)
        }

        return JPanel(BorderLayout()).apply {
            add(header, BorderLayout.NORTH)
            add(card, BorderLayout.CENTER)
        }
    }

    private fun show(rightPanel: JPanel, reloadResults: Boolean) {
        treeView.setStacked(isStacked())
        // only the scan tab can be collapsed; a finding always gets the room
        val collapsed = scanTab != null && pluginLocalState(project).scanOptionsCollapsed
        treeView.setSecondPaneCollapsed(collapsed, getCollapsedExtent())
        if (reloadResults) {
            treeView.refreshTree()
        }
        treeView.replaceRightPanel(rightPanel)
    }

    fun getTreeView() = treeView

    companion object {
        private const val COLLAPSED_HEIGHT = 30
        private const val COLLAPSED_WIDTH = 180
    }
}
