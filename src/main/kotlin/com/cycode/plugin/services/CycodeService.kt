package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliIgnoreType
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.AiRemediationResultData
import com.cycode.plugin.components.toolWindow.CycodeToolWindowFactory
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.components.toolWindow.updateToolWindowStateForAllProjects
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project


@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) : Disposable {
    private val cliService = cli(project)
    private val cliDownloadService = cliDownload()

    private val pluginState = pluginState()

    fun <T> runBackgroundTask(
        title: String,
        canBeCancelled: Boolean = true,
        task: (ProgressIndicator) -> T,
    ) {
        thisLogger().debug("Create background task: $title")
        object : Task.Backgroundable(project, title, canBeCancelled) {
            override fun run(indicator: ProgressIndicator) {
                thisLogger().debug("Run background task: $title")
                task(indicator)
                thisLogger().debug("Finish background task: $title")
            }
        }.queue()
        thisLogger().debug("Background task queued: $title")
    }

    fun installCliIfNeededAndCheckAuthentication() {
        runBackgroundTask(CycodeBundle.message("pluginLoading"), canBeCancelled = false) { _ ->
            thisLogger().debug("Check CLI installation and authentication")
            // we are using lock of download service because it shared per application
            // the current service is per project so, we can't create a lock here
            synchronized(cliDownloadService.initCliLock) {
                cliDownloadService.initCli()
                cliService.syncStatus()
                updateToolWindowState(project)
            }
        }
    }

    fun startAuth() {
        runBackgroundTask(CycodeBundle.message("authProcessing")) { indicator ->
            if (!pluginState.cliAuthed) {
                cliService.startAuth { indicator.isCanceled }
                cliService.syncStatus()
                updateToolWindowStateForAllProjects()
            }
        }
    }

    private fun getBackgroundScanningLabel(scanType: CliScanType) = when (scanType) {
        CliScanType.Secret -> CycodeBundle.message("secretScanning")
        CliScanType.Sca -> CycodeBundle.message("scaScanning")
        CliScanType.Iac -> CycodeBundle.message("iacScanning")
        CliScanType.Sast -> CycodeBundle.message("sastScanning")
    }

    fun startScanForCurrentProject(scanType: CliScanType) {
        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        // the only way to run the entire project scans is by pressing the button
        // so this is on demand scan
        startScan(scanType, listOf(projectRoot))
    }

    fun startScan(scanType: CliScanType, pathsToScan: List<String>, onDemand: Boolean = true) {
        runBackgroundTask(getBackgroundScanningLabel(scanType)) { indicator ->
            if (!pluginState.cliAuthed) {
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("authorizationRequiredNotification"))
                return@runBackgroundTask
            }

            thisLogger().debug("[$scanType] Start scanning paths: $pathsToScan")

            val cancelledCallback = { indicator.isCanceled }
            when (scanType) {
                CliScanType.Secret -> cliService.scanPathsSecrets(pathsToScan, onDemand, cancelledCallback)
                CliScanType.Sca -> cliService.scanPathsSca(pathsToScan, onDemand, cancelledCallback)
                CliScanType.Iac -> cliService.scanPathsIac(pathsToScan, onDemand, cancelledCallback)
                CliScanType.Sast -> cliService.scanPathsSast(pathsToScan, onDemand, cancelledCallback)
            }

            thisLogger().debug("[$scanType] Finish scanning paths: $pathsToScan")
        }
    }

    fun getAiRemediation(detectionId: String, onSuccess: (AiRemediationResultData) -> Unit) {
        runBackgroundTask(CycodeBundle.message("aiRemediationGenerating")) { indicator ->
            val aiRemediation = cliService.getAiRemediation(detectionId)
            if (aiRemediation != null) {
                onSuccess(aiRemediation)
            }
        }
    }

    private fun mapTypeToOptionName(type: CliIgnoreType): String {
        return when (type) {
            CliIgnoreType.VALUE -> "--by-value"
            CliIgnoreType.RULE -> "--by-rule"
            CliIgnoreType.PATH -> "--by-path"
        }
    }

    private fun applyIgnoreInUi(type: CliIgnoreType, value: String) {
        // exclude results from the local DB and restart the code analyzer

        val scanResults = scanResults(project)
        when (type) {
            CliIgnoreType.VALUE -> scanResults.excludeResults(byValue = value)
            CliIgnoreType.RULE -> scanResults.excludeResults(byRuleId = value)
            CliIgnoreType.PATH -> scanResults.excludeResults(byPath = value)
        }

        DaemonCodeAnalyzer.getInstance(project).restart()
        updateToolWindowState(project)
    }

    fun applyIgnoreFromFileAnnotation(scanType: CliScanType, type: CliIgnoreType, value: String) {
        // we are removing is from UI first to show how it's blazing fast and then apply it in the background
        applyIgnoreInUi(type, value)

        runBackgroundTask(CycodeBundle.message("ignoresApplying"), canBeCancelled = false) { indicator ->
            if (!pluginState.cliAuthed) {
                return@runBackgroundTask
            }

            cliService.ignore(
                scanType.name.lowercase(),
                mapTypeToOptionName(type),
                value,
                cancelledCallback = { indicator.isCanceled }
            )
        }
    }

    override fun dispose() {
        CycodeToolWindowFactory.TabManager.removeTab(project)
    }
}