package com.cycode.plugin.listeners

import com.cycode.plugin.Consts
import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.isSupportedPackageFile
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
import com.intellij.openapi.project.ProjectLocator
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.File
import java.util.concurrent.TimeUnit


class FileSaveListener(private val project: Project) : FileDocumentManagerListener {
    private val cliService = cli(project)
    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()
    private val collectedPathsToScan = mutableSetOf<String>() // we use a set to avoid duplicates

    init {
        thisLogger().debug("FileSaveListener init for project: ${project.name}")
        scheduleScanPathsFlush()
    }

    private fun scanSecretFlush(pathsToScan: List<String>) {
        object : Task.Backgroundable(project, CycodeBundle.message("secretScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                thisLogger().debug("[Secret] Start scanning paths: $pathsToScan")
                cliService.scanPathsSecrets(pathsToScan, onDemand = false, cancelledCallback = { indicator.isCanceled })
                thisLogger().debug("[Secret] Finish scanning paths: $pathsToScan")
            }
        }.queue()
    }

    private fun scanScaFlush(pathsToScan: List<String>) {
        object : Task.Backgroundable(project, CycodeBundle.message("scaScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                thisLogger().debug("[SCA] Start scanning paths: $pathsToScan")
                cliService.scanPathsSca(pathsToScan, onDemand = false, cancelledCallback = { indicator.isCanceled })
                thisLogger().debug("[SCA] Finish scanning paths: $pathsToScan")
            }
        }.queue()
    }

    private fun scanPathsFlush() {
        val pathsToScan = excludeNotExistingPaths(collectedPathsToScan.toMutableList())
        collectedPathsToScan.clear()

        if (!pluginState.cliAuthed) {
            return
        }

        if (pathsToScan.isNotEmpty()) {
            scanSecretFlush(pathsToScan)
        }

        val scaPathsToScan = excludeNonScaRelatedPaths(pathsToScan)
        if (Consts.EXPERIMENTAL_SCA_SUPPORT && scaPathsToScan.isNotEmpty()) {
            scanScaFlush(scaPathsToScan)
        }
    }

    private fun excludeNotExistingPaths(paths: List<String>): List<String> {
        return paths.filter { File(it).exists() }
    }

    private fun excludeNonScaRelatedPaths(paths: List<String>): List<String> {
        return paths.filter { isSupportedPackageFile(it) }
    }

    private fun scheduleScanPathsFlush() {
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            {
                scanPathsFlush()
            },
            Consts.PLUGIN_AUTO_SAVE_FLUSH_INITIAL_DELAY_SEC,
            Consts.PLUGIN_AUTO_SAVE_FLUSH_DELAY_SEC,
            TimeUnit.SECONDS
        )
    }

    override fun beforeDocumentSaving(document: Document) {
        thisLogger().debug("FileSaveListener.beforeDocumentSaving")

        if (!pluginSettings.scanOnSave || !pluginState.cliAuthed) {
            return
        }

        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val fileProject = ProjectLocator.getInstance().guessProjectForFile(file)

        if (project != fileProject) {
            thisLogger().debug("FileSaveListener.beforeDocumentSaving: project mismatch")
            return
        }

        val filePath = file.canonicalFile?.canonicalPath ?: return
        collectedPathsToScan.add(filePath)
    }
}
