package com.cycode.plugin.components.toolWindow.components.scanContentTab.components.treeView

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JSplitPane
import javax.swing.border.Border
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI

/**
 * Makes a split pane invisible. Only contained components are shown.
 *
 * Ref: https://stackoverflow.com/a/12799814/8032027
 *
 * @param splitPane
 */
fun flattenJSplitPane(splitPane: JSplitPane) {
    splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    val flatDividerSplitPaneUI: BasicSplitPaneUI = object : BasicSplitPaneUI() {
        override fun createDefaultDivider(): BasicSplitPaneDivider {
            return object : BasicSplitPaneDivider(this) {
                override fun setBorder(b: Border?) {}
            }
        }
    }
    splitPane.setUI(flatDividerSplitPaneUI)
    splitPane.setBorder(null)
}

fun openFileInEditor(project: Project, filePath: String, lineNumber: Int) {
    val file = File(filePath)
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

    val editorManager = FileEditorManager.getInstance(project)
    editorManager.openTextEditor(
        OpenFileDescriptor(project, virtualFile, lineNumber, 0), true
    )
}
