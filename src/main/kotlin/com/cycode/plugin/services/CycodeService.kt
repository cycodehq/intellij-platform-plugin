package com.cycode.plugin.services

import com.cycode.plugin.Consts
import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager


@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) {
    private val cliService = cli(project)
    private val cliDownloadService = cliDownload()

    private val pluginSettings = pluginSettings()
    private val pluginState = pluginState()

    fun installCliIfNeededAndCheckAuthentication() {
        object : Task.Backgroundable(project, CycodeBundle.message("pluginLoading"), false) {
            override fun run(indicator: ProgressIndicator) {
                // if the CLI path is not overriden and executable is auto managed, and need to download - download it.
                if (
                    pluginSettings.cliPath == Consts.DEFAULT_CLI_PATH &&
                    pluginSettings.cliAutoManaged &&
                    cliDownloadService.shouldDownloadCli(pluginSettings.cliPath)
                ) {
                    cliDownloadService.downloadCli(pluginSettings.cliPath)
                    thisLogger().info("CLI was successfully downloaded/updated")
                }

                // required to know CLI version.
                // unfortunately, we don't have a universal command that will cover the auth state and CLI version yet
                cliService.healthCheck()

                cliService.checkAuth()
                updateToolWindowState(project)
            }
        }.queue()
    }

    fun startAuth() {
        object : Task.Backgroundable(project, CycodeBundle.message("authProcessing"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    cliService.cliShouldDestroyCallback = { indicator.isCanceled }

                    val successLogin = cliService.doAuth()
                    pluginState.cliAuthed = successLogin

                    updateToolWindowState(project)
                }
            }
        }.queue()
    }

    fun startSecretScanForFile(filepath: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("fileScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.cliShouldDestroyCallback = { indicator.isCanceled }
                cliService.scanPathsSecrets(listOf(filepath))
            }
        }.queue()
    }

    fun applyIgnoreFromFileAnnotation(filepath: String, optionName: String, optionValue: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("ignoresApplying"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.cliShouldDestroyCallback = { indicator.isCanceled }

                cliService.ignore(optionName, optionValue)

                // same trick as in our vs code extension
                // the right way: apply "ignore rules" in the local results db of the plugin
                // the disadvantage of the right way: we rewrite code that already exists in CLI in every plugin...
                // TODO(MarshalX): think about what we can do from CLI side
                cliService.scanPathsSecrets(listOf(filepath))
            }
        }.queue()
    }

    fun startSecretScanForCurrentFile() {
        val currentOpenedDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
        if (currentOpenedDocument == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noOpenFileErrorNotification"))
            return
        }

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(currentOpenedDocument)
        val vFile = psiFile!!.originalFile.virtualFile

        startSecretScanForFile(vFile.path)
    }
}
