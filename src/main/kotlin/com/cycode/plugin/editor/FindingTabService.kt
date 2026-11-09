package com.cycode.plugin.editor

import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import javax.swing.SwingConstants

/**
 * Opens findings in one editor tab on the right of the editor area, so they get more room than the tool window.
 * The tab is reused: selecting another finding replaces its content.
 */
@Service(Service.Level.PROJECT)
class FindingTabService(private val project: Project) {
    private val file = FindingVirtualFile()

    fun show(card: JComponent) {
        val manager = FileEditorManager.getInstance(project)
        if (manager.isFileOpen(file)) {
            // bring the existing tab forward without taking the focus from the tree
            manager.openFile(file, false)
        } else {
            openOnTheRight()
        }

        manager.getEditors(file).filterIsInstance<FindingFileEditor>().forEach { it.showCard(card) }
    }

    private fun openOnTheRight() {
        val manager = FileEditorManagerEx.getInstanceEx(project)
        val current = manager.currentWindow
        if (current == null) {
            manager.openFile(file, false)
            return
        }

        // reuse an existing split to the right instead of creating another one every time
        var other = manager.getNextWindow(current)?.takeIf { it != current }
        if (other == null) {
            manager.createSplitter(SwingConstants.VERTICAL, current)
            other = manager.getNextWindow(current)?.takeIf { it != current }
        }
        if (other == null) {
            manager.openFile(file, false)
            return
        }
        manager.openFile(file, other, FileEditorOpenOptions().withRequestFocus(false))
    }
}
