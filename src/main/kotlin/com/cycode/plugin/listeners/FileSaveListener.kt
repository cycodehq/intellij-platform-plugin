package com.cycode.plugin.listeners

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.managers.CliManager
import com.cycode.plugin.services.pluginSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project


class FileSaveListener(private val project: Project) : FileDocumentManagerListener {
    private val cliManager = CliManager()
    private val pluginSettings = pluginSettings()

    override fun beforeDocumentSaving(document: Document) {
        thisLogger().debug("FileSaveListener.beforeDocumentSaving")

        if (!pluginSettings.scanOnSave) {
            return
        }

        val filePath = FileDocumentManager.getInstance().getFile(document)?.canonicalFile?.canonicalPath ?: return

        object : Task.Backgroundable(project, CycodeBundle.message("fileScanning"), false) {
            override fun run(indicator: ProgressIndicator) {
                thisLogger().debug("Start scanning file: $filePath")
                cliManager.scanFile(filePath)
                thisLogger().debug("Finish scanning file: $filePath")
            }
        }.queue()
    }
}
