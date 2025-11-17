package com.cycode.plugin.components.toolWindow.components.treeView.components.detectionNodeContextMenu

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.components.toolWindow.components.treeView.TreeView
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.cycode.plugin.components.toolWindow.components.treeView.openDetectionInFile
import com.cycode.plugin.services.cycode
import com.intellij.openapi.editor.actions.ContentChooser.RETURN_SYMBOL
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.text.StringUtil.convertLineSeparators
import com.intellij.openapi.util.text.StringUtil.first
import com.intellij.ui.ColoredListCellRenderer
import org.jetbrains.annotations.Nls
import java.awt.event.MouseEvent
import javax.swing.JList
import javax.swing.ListSelectionModel.SINGLE_SELECTION
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

val RUN_OPTION = CycodeBundle.message("runOption")
val RESCAN_OPTION = CycodeBundle.message("rescanOption")
val OPEN_IN_EDITOR_OPTION = CycodeBundle.message("openInEditorOption")
val OPEN_VIOLATION_CARD_OPTION = CycodeBundle.message("openViolationCardOption")

class DetectionNodeContextMenu(
    private val treeView: TreeView,
    private val project: Project,
    private val treePath: TreePath
) {
    private val service = cycode(project)

    private fun getUnknownNode(): Any {
        if (treePath.lastPathComponent !is DefaultMutableTreeNode) {
            throw IllegalArgumentException("treePath.lastPathComponent is not a DefaultMutableTreeNode")
        }

        return (treePath.lastPathComponent as DefaultMutableTreeNode).userObject
    }

    private fun createChosenOptions(): List<String> {
        return when (getUnknownNode()) {
            is ScanTypeNode -> listOf(RUN_OPTION)
            is SecretDetectionNode -> listOf(OPEN_VIOLATION_CARD_OPTION, OPEN_IN_EDITOR_OPTION, RESCAN_OPTION)
            is ScaDetectionNode -> listOf(OPEN_VIOLATION_CARD_OPTION, OPEN_IN_EDITOR_OPTION, RESCAN_OPTION)
            is IacDetectionNode -> listOf(OPEN_VIOLATION_CARD_OPTION, OPEN_IN_EDITOR_OPTION, RESCAN_OPTION)
            is SastDetectionNode -> listOf(OPEN_VIOLATION_CARD_OPTION, OPEN_IN_EDITOR_OPTION, RESCAN_OPTION)
            else -> listOf()
        }
    }

    private fun onChosenOptionClicked(chosenOption: String) {
        when (chosenOption) {
            RUN_OPTION -> onRunOptionClicked()
            RESCAN_OPTION -> onRescanOptionClicked()
            OPEN_IN_EDITOR_OPTION -> onOpenInEditorOptionClicked()
            OPEN_VIOLATION_CARD_OPTION -> onOpenViolationCardOptionClicked()
        }
    }

    private fun onRunOptionClicked() {
        val node = getUnknownNode()
        if (node !is ScanTypeNode) {
            return
        }

        // FIXME(MarshalX): add some key field instead of abusing name?
        when (node.name) {
            CycodeBundle.message("secretDisplayName") -> service.startScanForCurrentProject(CliScanType.Secret)
            CycodeBundle.message("scaDisplayName") -> service.startScanForCurrentProject(CliScanType.Sca)
            CycodeBundle.message("iacDisplayName") -> service.startScanForCurrentProject(CliScanType.Iac)
            CycodeBundle.message("sastDisplayName") -> service.startScanForCurrentProject(CliScanType.Sast)
        }
    }

    private fun onRescanOptionClicked() {
        when (val node = getUnknownNode()) {
            is SecretDetectionNode -> service.startScan(
                CliScanType.Secret,
                listOf(node.detection.detectionDetails.getFilepath()),
            )

            is ScaDetectionNode -> service.startScan(
                CliScanType.Sca,
                listOf(node.detection.detectionDetails.getFilepath()),
            )

            is IacDetectionNode -> service.startScan(
                CliScanType.Iac,
                listOf(node.detection.detectionDetails.getFilepath()),
            )

            is SastDetectionNode -> service.startScan(
                CliScanType.Sast,
                listOf(node.detection.detectionDetails.getFilepath()),
            )
        }
    }

    private fun onOpenInEditorOptionClicked() {
        val node = getUnknownNode()
        if (node is AbstractNode) {
            openDetectionInFile(project, node)
        }
    }

    private fun onOpenViolationCardOptionClicked() {
        val node = getUnknownNode()
        if (node is AbstractNode) {
            treeView.displayViolationCard(node)
        }
    }

    fun showPopup(e: MouseEvent) {
        val options = createChosenOptions()
        if (options.isEmpty()) {
            return
        }

        val rightMargin = 50
        JBPopupFactory.getInstance().createPopupChooserBuilder(options)
            .setVisibleRowCount(7)
            .setSelectionMode(SINGLE_SELECTION)
            .setItemChosenCallback { onChosenOptionClicked(it) }
            .setRenderer(object : ColoredListCellRenderer<String>() {
                override fun customizeCellRenderer(
                    list: JList<out String>,
                    value: @Nls String,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    append(first(convertLineSeparators(value, RETURN_SYMBOL), rightMargin, true))
                }
            })
            .addListener(object : JBPopupListener {
                override fun beforeShown(event: LightweightWindowEvent) {
                    val popup = event.asPopup()
                    popup.setLocation(e.locationOnScreen)
                }
            })
            .setNamerForFiltering { it }
            .setAutoPackHeightOnFiltering(false)
            .createPopup()
            .show(e.component)
    }
}
