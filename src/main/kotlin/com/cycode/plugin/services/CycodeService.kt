package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.managers.CliManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager


@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) {
    private val cliManager = CliManager(project)

    private val pluginState = pluginState()

    init {
        thisLogger().info(CycodeBundle.message("projectService", project.name))
    }

    fun startAuth() {
        object : Task.Backgroundable(project, CycodeBundle.message("authProcessing"), false) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    val successLogin = cliManager.doAuth()
                    pluginState.cliAuthed = successLogin

                    updateToolWindowState(project)
                }
            }
        }.queue()
    }

    fun startSecretScanForFile(filepath: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("fileScanning"), false) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliManager.scanFileSecrets(filepath)
            }
        }.queue()
    }

    fun applyIgnoreFromFileAnnotation(filepath: String, optionName: String, optionValue: String) {
        object : Task.Backgroundable(project, CycodeBundle.message("ignoresApplying"), false) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliManager.ignore(optionName, optionValue)

                // same trick as in our vs code extension
                // the right way: apply "ignore rules" in the local results db of the plugin
                // the disadvantage of the right way: we rewrite code that already exists in CLI in every plugin...
                // TODO(MarshalX): think about what we can do from CLI side
                cliManager.scanFileSecrets(filepath)
            }
        }.queue()
    }

    fun startSecretScanForCurrentFile() {
        val currentOpenedDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document ?: return

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(currentOpenedDocument)
        val vFile = psiFile!!.originalFile.virtualFile

        startSecretScanForFile(vFile.path)
    }
}
