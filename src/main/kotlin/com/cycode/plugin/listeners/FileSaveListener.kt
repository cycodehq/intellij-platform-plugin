package com.cycode.plugin.listeners

import com.cycode.plugin.Consts
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.isSupportedIacFile
import com.cycode.plugin.cli.isSupportedPackageFile
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.pluginLocalState
import com.cycode.plugin.services.pluginSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.File
import java.util.concurrent.TimeUnit


class FileSaveListener(private val project: Project) : FileDocumentManagerListener {
    private val service = cycode(project)
    private val pluginLocalState = pluginLocalState(project)
    private val pluginSettings = pluginSettings()
    private val collectedPathsToScan = mutableSetOf<String>() // we use a set to avoid duplicates

    init {
        thisLogger().debug("FileSaveListener init for project: ${project.name}")
        scheduleScanPathsFlush()
    }

    private fun scanPathsFlush() {
        val pathsToScan = excludeNotExistingPaths(collectedPathsToScan.toMutableList())
        collectedPathsToScan.clear()

        if (!pluginLocalState.cliAuthed) {
            return
        }

        if (pluginLocalState.isSecretScanningEnabled && pathsToScan.isNotEmpty()) {
            service.startScan(CliScanType.Secret, pathsToScan, onDemand = false)
        }

        val scaPathsToScan = excludeNonScaRelatedPaths(pathsToScan)
        if (pluginLocalState.isScaScanningEnabled && scaPathsToScan.isNotEmpty()) {
            service.startScan(CliScanType.Sca, scaPathsToScan, onDemand = false)
        }

        val iacPathsToScan = excludeNonIacRelatedPaths(pathsToScan)
        if (pluginLocalState.isIacScanningEnabled && iacPathsToScan.isNotEmpty()) {
            service.startScan(CliScanType.Iac, iacPathsToScan, onDemand = false)
        }
    }

    private fun excludeNotExistingPaths(paths: List<String>): List<String> {
        return paths.filter { File(it).exists() }
    }

    private fun excludeNonScaRelatedPaths(paths: List<String>): List<String> {
        return paths.filter { isSupportedPackageFile(it) }
    }

    private fun excludeNonIacRelatedPaths(paths: List<String>): List<String> {
        return paths.filter { isSupportedIacFile(it) }
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

        if (!pluginSettings.scanOnSave || !pluginLocalState.cliAuthed) {
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
