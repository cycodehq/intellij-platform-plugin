package com.cycode.plugin.editor

import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import javax.swing.SwingConstants

/**
 * Opens findings in one editor tab in a split next to the open file, so they get more room than the tool window.
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
            openInAnotherWindow()
        }

        manager.getEditors(file).filterIsInstance<FindingFileEditor>().forEach { it.showCard(card) }
    }

    /**
     * Makes an editor window other than the finding tab's the current one, so a source file opens there instead of
     * next to the finding. Does nothing while the finding tab is not open or another window is already current.
     */
    fun focusSourceWindow() {
        val manager = FileEditorManagerEx.getInstanceEx(project)
        if (!manager.isFileOpen(file)) return
        if (manager.currentWindow?.isFileOpen(file) != true) return

        manager.windows.firstOrNull { !it.isFileOpen(file) }?.setAsCurrentWindow(false)
    }

    private fun openInAnotherWindow() {
        val manager = FileEditorManagerEx.getInstanceEx(project)
        val current = manager.currentWindow
        if (current == null) {
            manager.openFile(file, false)
            return
        }

        // reuse another editor window if there is one, instead of creating a split every time
        var other = manager.windows.firstOrNull { it != current }
        // createSplitter starts the new window with a copy of the file that was open (not part of its documented
        // contract, so it is checked by hand in the IDE and the copy is closed below)
        val copiedFile = current.selectedFile
        var splitCreated = false
        if (other == null) {
            manager.createSplitter(SwingConstants.VERTICAL, current)
            other = manager.windows.firstOrNull { it != current }
            splitCreated = true
        }
        if (other == null) {
            manager.openFile(file, false)
            return
        }
        // opens in the current window: FileEditorOpenOptions cannot be built in a way every IDE version resolves
        other.setAsCurrentWindow(false)
        FileEditorManager.getInstance(project).openFile(file, false)

        // close the copy only after the finding tab is open, so the new split is not removed for being empty
        if (splitCreated && copiedFile != null && copiedFile != file) {
            manager.closeFile(copiedFile, other)
        }
    }
}
