package com.cycode.plugin.components.toolWindow.components.treeView

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File


fun openFileInEditor(project: Project, filePath: String, lineNumber: Int) {
    val file = File(filePath)
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

    val editorManager = FileEditorManager.getInstance(project)
    editorManager.openTextEditor(
        OpenFileDescriptor(project, virtualFile, lineNumber, 0), true
    )
}
