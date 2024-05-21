package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliIgnoreType
import com.cycode.plugin.cli.CliScanType
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

    fun installCliIfNeededAndCheckAuthentication() {
        object : Task.Backgroundable(project, CycodeBundle.message("pluginLoading"), false) {
            override fun run(indicator: ProgressIndicator) {
                // we are using lock of download service because it shared per application
                // the current service is per project so, we can't create a lock here
                synchronized(cliDownloadService.initCliLock) {
                    cliDownloadService.initCli()

                    // required to know CLI version.
                    // we don't have a universal command that will cover the auth state and CLI version yet
                    cliService.healthCheck()

                    cliService.checkAuth()
                    updateToolWindowState(project)
                }
            }
        }.queue()
    }

    fun startAuth() {
        object : Task.Backgroundable(project, CycodeBundle.message("authProcessing"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    val successLogin = cliService.doAuth { indicator.isCanceled }
                    pluginState.cliAuthed = successLogin

                    updateToolWindowStateForAllProjects()
                }
            }
        }.queue()
    }

    fun startPathSecretScan(path: String, onDemand: Boolean = false) {
        startPathSecretScan(listOf(path), onDemand = onDemand)
    }

    fun startPathSecretScan(pathsToScan: List<String>, onDemand: Boolean = false) {
        object : Task.Backgroundable(project, CycodeBundle.message("secretScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                thisLogger().debug("[Secret] Start scanning paths: $pathsToScan")
                cliService.scanPathsSecrets(
                    pathsToScan,
                    onDemand = onDemand,
                    cancelledCallback = { indicator.isCanceled })
                thisLogger().debug("[Secret] Finish scanning paths: $pathsToScan")
            }
        }.queue()
    }

    fun startPathScaScan(path: String, onDemand: Boolean = false) {
        startPathScaScan(listOf(path), onDemand = onDemand)
    }

    fun startPathScaScan(pathsToScan: List<String>, onDemand: Boolean = false) {
        object : Task.Backgroundable(project, CycodeBundle.message("scaScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                thisLogger().debug("[SCA] Start scanning paths: $pathsToScan")
                cliService.scanPathsSca(
                    pathsToScan,
                    onDemand = onDemand,
                    cancelledCallback = { indicator.isCanceled }
                )
                thisLogger().debug("[SCA] Finish scanning paths: $pathsToScan")
            }
        }.queue()
    }

    fun startPathIacScan(path: String, onDemand: Boolean = false) {
        startPathIacScan(listOf(path), onDemand = onDemand)
    }

    fun startPathIacScan(pathsToScan: List<String>, onDemand: Boolean = false) {
        object : Task.Backgroundable(project, CycodeBundle.message("iacScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                thisLogger().debug("[IAC] Start scanning paths: $pathsToScan")
                cliService.scanPathsIac(
                    pathsToScan,
                    onDemand = onDemand,
                    cancelledCallback = { indicator.isCanceled }
                )
                thisLogger().debug("[IAC] Finish scanning paths: $pathsToScan")
            }
        }.queue()
    }

    fun startPathSastScan(path: String, onDemand: Boolean = false) {
        startPathSastScan(listOf(path), onDemand = onDemand)
    }

    fun startPathSastScan(pathsToScan: List<String>, onDemand: Boolean = false) {
        object : Task.Backgroundable(project, CycodeBundle.message("sastScanning"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                thisLogger().debug("[SAST] Start scanning paths: $pathsToScan")
                cliService.scanPathsSast(
                    pathsToScan,
                    onDemand = onDemand,
                    cancelledCallback = { indicator.isCanceled }
                )
                thisLogger().debug("[SAST] Finish scanning paths: $pathsToScan")
            }
        }.queue()
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

        object : Task.Backgroundable(project, CycodeBundle.message("ignoresApplying"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!pluginState.cliAuthed) {
                    return
                }

                cliService.ignore(
                    scanType.name.toLowerCase(),
                    mapTypeToOptionName(type),
                    value,
                    cancelledCallback = { indicator.isCanceled })
            }
        }.queue()
    }

    fun startSecretScanForCurrentProject() {
        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        startPathSecretScan(projectRoot, onDemand = true)
    }

    fun startScaScanForCurrentProject() {
        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        startPathScaScan(projectRoot, onDemand = true)
    }

    fun startIacScanForCurrentProject() {
        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        startPathIacScan(projectRoot, onDemand = true)
    }

    fun startSastScanForCurrentProject() {
        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        startPathSastScan(projectRoot, onDemand = true)
    }

    override fun dispose() {
        CycodeToolWindowFactory.TabManager.removeTab(project)
    }
}
