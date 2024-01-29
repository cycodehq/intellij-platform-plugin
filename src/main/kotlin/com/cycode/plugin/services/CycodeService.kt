package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager


@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) {
    private val cliService = cli(project)
    private val cliDownloadService = cliDownload()

    private val pluginState = pluginState()

    fun installCliIfNeededAndCheckAuthentication() {
        object : Task.Backgroundable(project, CycodeBundle.message("pluginLoading"), false) {
            override fun run(indicator: ProgressIndicator) {
                cliDownloadService.initCli()

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
                    val successLogin = cliService.doAuth { indicator.isCanceled }
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

                cliService.scanPathsSecrets(listOf(path), cancelledCallback = { indicator.isCanceled })
            }
        }.queue()
    }

    private fun startPathScaScan(path: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("scaScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.scanPathsSca(listOf(path), cancelledCallback = { indicator.isCanceled })
            }
        }.queue()
    }

    fun applyIgnoreFromFileAnnotation(optionName: String, optionValue: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("ignoresApplying"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.ignore(optionName, optionValue, cancelledCallback = { indicator.isCanceled })
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
