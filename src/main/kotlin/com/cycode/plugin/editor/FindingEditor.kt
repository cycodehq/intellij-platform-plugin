package com.cycode.plugin.editor

import com.cycode.plugin.icons.PluginIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

const val FINDING_TAB_NAME = "Cycode Vulnerability"
private const val LEFT_PADDING = 12

object FindingFileType : FileType {
    override fun getName() = "CycodeFinding"
    override fun getDescription() = FINDING_TAB_NAME
    override fun getDefaultExtension() = ""
    override fun getIcon(): Icon = PluginIcons.TOOL_WINDOW
    override fun isBinary() = true
    override fun isReadOnly() = true
}

/** The single in-memory file behind the finding tab; it never exists on disk. */
class FindingVirtualFile : LightVirtualFile(FINDING_TAB_NAME, FindingFileType, "")

/** Shows one violation card; the card is replaced when another finding is selected. */
class FindingFileEditor(private val file: FindingVirtualFile) : UserDataHolderBase(), FileEditor {
    private val holder = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.emptyLeft(LEFT_PADDING)
    }

    fun showCard(card: JComponent) {
        holder.removeAll()
        holder.add(card, BorderLayout.CENTER)
        holder.revalidate()
        holder.repaint()
    }

    override fun getComponent(): JComponent = holder
    override fun getPreferredFocusedComponent(): JComponent? = null
    override fun getName(): String = FINDING_TAB_NAME
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) {}
    override fun isModified() = false
    override fun isValid() = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? = null
    override fun dispose() {}
}

class FindingFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile) = file is FindingVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return FindingFileEditor(file as FindingVirtualFile)
    }

    override fun getEditorTypeId() = "cycode-finding"

    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
