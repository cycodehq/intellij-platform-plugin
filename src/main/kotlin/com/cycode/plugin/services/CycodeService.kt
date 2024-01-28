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
                    cliDownloadService.shouldDownloadCli()
                ) {
                    cliDownloadService.downloadCli()
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

    private fun startPathSecretScan(path: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("secretScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.cliShouldDestroyCallback = { indicator.isCanceled }
                cliService.scanPathsSecrets(listOf(path))
            }
        }.queue()
    }

    private fun startPathScaScan(path: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("scaScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.cliShouldDestroyCallback = { indicator.isCanceled }
                cliService.scanPathsSca(listOf(path))
            }
        }.queue()
    }

    fun applyIgnoreFromFileAnnotation(optionName: String, optionValue: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("ignoresApplying"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.cliShouldDestroyCallback = { indicator.isCanceled }
                cliService.ignore(optionName, optionValue)
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

        startPathSecretScan(vFile.path)
    }

    fun startScaScanForCurrentProject() {
        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        startPathScaScan(projectRoot)
    }
}
