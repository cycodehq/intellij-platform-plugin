package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
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

    private fun getCollapsedExtent(stacked: Boolean) = JBUI.scale(if (stacked) COLLAPSED_HEIGHT else COLLAPSED_WIDTH)

    /**
     * Replaces the right panel with the one for the current plugin state (loading, authentication or scan).
     * [reloadResults] = false keeps the results tree (and its expanded nodes) untouched, for layout-only changes.
     */
    fun showStatePanel(reloadResults: Boolean = true) {
        val service = cycode(project)
        val local = pluginLocalState(project)

        val stacked = isToolWindowStacked(project)
        scanTab = null
        val panel = when {
            !local.cliInstalled -> LoadingContentTab().getContent(service)
            local.cliAuthed -> ScanContentTab(stacked).also { scanTab = it }.getContent(service)
            else -> AuthContentTab().getContent(service)
        }
        show(panel, stacked, reloadResults)
    }

    /**
     * Only the scan buttons and the spinners in the tree change; whatever is shown is never replaced.
     * [startedScanType] is a scan that has just started: its results rows start over, collapsed.
     */
    fun refreshRunningState(startedScanType: CliScanType? = null) {
        startedScanType?.let { treeView.collapseScanType(it) }
        scanTab?.refreshRunningState(project)
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
        show(wrapWithCloseButton(card), isToolWindowStacked(project), reloadResults = false)
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

    private fun show(rightPanel: JPanel, stacked: Boolean, reloadResults: Boolean) {
        treeView.setStacked(stacked)
        // only the scan tab can be collapsed; a finding always gets the room
        val collapsed = scanTab != null && pluginLocalState(project).scanOptionsCollapsed
        treeView.setSecondPaneCollapsed(collapsed, getCollapsedExtent(stacked))
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
