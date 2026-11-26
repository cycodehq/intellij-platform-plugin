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

const val DIFFERENCE_BETWEEN_CYCODE_AND_IDE_LINE_NUMBER = 1

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
    val line = node.detection.detectionDetails.getLineNumber() - DIFFERENCE_BETWEEN_CYCODE_AND_IDE_LINE_NUMBER
    openFileInEditor(project, filePath, line)
}

private fun openScaDetectionInFile(project: Project, node: ScaDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.getLineNumber() - DIFFERENCE_BETWEEN_CYCODE_AND_IDE_LINE_NUMBER
    openFileInEditor(project, filePath, line)
}

private fun openIacDetectionInFile(project: Project, node: IacDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.getLineNumber() - DIFFERENCE_BETWEEN_CYCODE_AND_IDE_LINE_NUMBER
    openFileInEditor(project, filePath, line)
}

private fun openSastDetectionInFile(project: Project, node: SastDetectionNode) {
    val filePath = node.detection.detectionDetails.getFilepath()
    val line = node.detection.detectionDetails.getLineNumber() - DIFFERENCE_BETWEEN_CYCODE_AND_IDE_LINE_NUMBER
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

/**
 * The path to show in the tree: relative to the project root where the file is inside it.
 *
 * Never throws. Results do not always come back as absolute paths: IaC detections can lack the leading slash
 * ("Users/me/project/Dockerfile"), and a project without a base path has no root to be relative to. A path that
 * cannot be made relative is shown as it is.
 */
fun toProjectRelativePath(filePath: String, projectRoot: File): String {
    if (!projectRoot.isAbsolute) return filePath

    var file = File(filePath)
    if (!file.isAbsolute) {
        val rooted = File("/$filePath")
        if (!rooted.startsWith(projectRoot)) return filePath
        file = rooted
    }

    return try {
        file.relativeTo(projectRoot).path
    } catch (e: IllegalArgumentException) {
        // different roots (e.g. another drive)
        filePath
    }
}
