package com.cycode.plugin.listeners

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.services.cli
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.services.pluginState
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project


class FileSaveListener(private val project: Project) : FileDocumentManagerListener {
    private val cliService = cli(project)
    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()

    override fun beforeDocumentSaving(document: Document) {
        thisLogger().debug("FileSaveListener.beforeDocumentSaving")

        if (!pluginSettings.scanOnSave || !pluginState.cliAuthed) {
            return
        }

        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val filePath = file.canonicalFile?.canonicalPath ?: return

        object : Task.Backgroundable(project, CycodeBundle.message("fileScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                cliService.cliShouldDestroyCallback = { indicator.isCanceled }

                thisLogger().debug("Start scanning file: $filePath")
                cliService.scanFileSecrets(filePath, false)
                thisLogger().debug("Finish scanning file: $filePath")
            }
        }.queue()
    }
}
