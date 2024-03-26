package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.components.toolWindow.components.treeView.nodes.IacDetectionNode
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.ScaDetectionNode
import com.cycode.plugin.components.toolWindow.components.treeView.nodes.SecretDetectionNode
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

const val DIFFERENCE_BETWEEN_SCA_LINE_NUMBERS = 1
const val DIFFERENCE_BETWEEN_IAC_LINE_NUMBERS = 1

private fun openFileInEditor(project: Project, filePath: String, lineNumber: Int) {
    val file = File(filePath)
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

    val editorManager = FileEditorManager.getInstance(project)
    editorManager.openTextEditor(
        OpenFileDescriptor(project, virtualFile, lineNumber, 0), true
    )
}

fun openSecretDetectionInFile(project: Project, node: SecretDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.line
    openFileInEditor(project, filePath, line)
}

fun openScaDetectionInFile(project: Project, node: ScaDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.lineInFile - DIFFERENCE_BETWEEN_SCA_LINE_NUMBERS
    openFileInEditor(project, filePath, line)
}

fun openIacDetectionInFile(project: Project, node: IacDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.lineInFile - DIFFERENCE_BETWEEN_IAC_LINE_NUMBERS
    openFileInEditor(project, filePath, line)
}
