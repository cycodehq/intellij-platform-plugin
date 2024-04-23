package com.cycode.plugin.components.toolWindow.components.treeView

import com.cycode.plugin.components.toolWindow.components.treeView.nodes.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File

const val DIFFERENCE_BETWEEN_SCA_LINE_NUMBERS = 1
const val DIFFERENCE_BETWEEN_IAC_LINE_NUMBERS = 1
const val DIFFERENCE_BETWEEN_SAST_LINE_NUMBERS = 1

fun getPsiFile(project: Project, filePath: String): PsiFile? {
    val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$filePath") ?: return null
    return PsiManager.getInstance(project).findFile(virtualFile)
}

private fun openFileInEditor(project: Project, filePath: String, lineNumber: Int) {
    val file = File(filePath)
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

    val editorManager = FileEditorManager.getInstance(project)
    editorManager.openTextEditor(
        OpenFileDescriptor(project, virtualFile, lineNumber, 0), true
    )
}

private fun openSecretDetectionInFile(project: Project, node: SecretDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.line
    openFileInEditor(project, filePath, line)
}

private fun openScaDetectionInFile(project: Project, node: ScaDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.lineInFile - DIFFERENCE_BETWEEN_SCA_LINE_NUMBERS
    openFileInEditor(project, filePath, line)
}

private fun openIacDetectionInFile(project: Project, node: IacDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.lineInFile - DIFFERENCE_BETWEEN_IAC_LINE_NUMBERS
    openFileInEditor(project, filePath, line)
}

private fun openSastDetectionInFile(project: Project, node: SastDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.lineInFile - DIFFERENCE_BETWEEN_SAST_LINE_NUMBERS
    openFileInEditor(project, filePath, line)
}

fun openDetectionInFile(project: Project, node: AbstractNode) {
    when (node) {
        is SecretDetectionNode -> openSecretDetectionInFile(project, node)
        is ScaDetectionNode -> openScaDetectionInFile(project, node)
        is IacDetectionNode -> openIacDetectionInFile(project, node)
        is SastDetectionNode -> openSastDetectionInFile(project, node)
    }
}
