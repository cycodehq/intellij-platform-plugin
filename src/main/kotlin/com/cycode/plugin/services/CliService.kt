package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.*
import com.cycode.plugin.cli.models.AuthCheckResult
import com.cycode.plugin.cli.models.AuthResult
import com.cycode.plugin.cli.models.VersionResult
import com.cycode.plugin.cli.models.scanResult.sca.ScaScanResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project


typealias TaskCancelledCallback = (() -> Boolean)?

@Service(Service.Level.PROJECT)
class CliService(private val project: Project) {
    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()

    private val scanResults = scanResults()

    private val cli = CliWrapper(pluginSettings.cliPath, getProjectRootDirectory())

    fun getProjectRootDirectory(): String? {
        val modules = ModuleManager.getInstance(project).modules
        if (modules.isEmpty()) {
            return null
        }

        val module = modules[0]
        return module.project.basePath
    }

    private fun resetPluginCLiState() {
        pluginState.cliAuthed = false
        pluginState.cliInstalled = false
        pluginState.cliVer = null
    }

    private fun showErrorNotification(message: String) {
        CycodeNotifier.notifyError(project, message)
    }

    private fun <T> processResult(result: CliResult<T>): CliResult<T>? {
        if (result is CliResult.Error) {
            showErrorNotification(result.result.message)
            return null
        }
        if (result is CliResult.Panic) {
            if (result.exitCode == ExitCodes.TERMINATION) {
                // don't notify user about user-requested terminations
                return null
            }

            showErrorNotification(result.errorMessage)
            return null
        }

        return result
    }

    fun healthCheck(cancelledCallback: TaskCancelledCallback = null): Boolean {
        val result: CliResult<VersionResult> =
            cli.executeCommand(
                "version",
                cancelledCallback = cancelledCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginState.cliInstalled = true
            pluginState.cliVer = processedResult.result.version
            return true
        }

        resetPluginCLiState()
        return false
    }

    fun checkAuth(cancelledCallback: TaskCancelledCallback = null): Boolean {
        val result: CliResult<AuthCheckResult> =
            cli.executeCommand(
                "auth",
                "check",
                cancelledCallback = cancelledCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginState.cliInstalled = true
            pluginState.cliAuthed = processedResult.result.result
            if (!pluginState.cliAuthed) {
                showErrorNotification(CycodeBundle.message("checkAuthErrorNotification"))
            }

            return pluginState.cliAuthed
        }

        resetPluginCLiState()
        return false
    }

    fun doAuth(cancelledCallback: TaskCancelledCallback = null): Boolean {
        val result: CliResult<AuthResult> =
            cli.executeCommand(
                "auth",
                cancelledCallback = cancelledCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginState.cliAuthed = processedResult.result.result
            if (!pluginState.cliAuthed) {
                showErrorNotification(CycodeBundle.message("authErrorNotification"))
            }
            return pluginState.cliAuthed
        }

        return false
    }

    fun ignore(
        optionScanType: String,
        optionName: String,
        optionValue: String,
        cancelledCallback: TaskCancelledCallback = null
    ): Boolean {
        val result: CliResult<Unit> = cli.executeCommand(
            "ignore",
            "-t",
            optionScanType,
            optionName,
            optionValue,
            cancelledCallback = cancelledCallback
        )

        val processedResult = processResult(result)
        return processedResult is CliResult.Success
    }

    private fun showScanFileResultNotification(scanType: CliScanType, detectionsCount: Int, onDemand: Boolean) {
        val scanTypeName = getScanTypeDisplayName(scanType)

        if (detectionsCount > 0) {
            val message = CycodeBundle.message("scanFileResultNotification", detectionsCount, scanTypeName)
            CycodeNotifier.notifyDetections(project, message)
        } else if (onDemand) {
            val message = CycodeBundle.message("scanFileNoResultNotification", scanTypeName)
            CycodeNotifier.notifyInfo(project, message)
        }
    }

    private inline fun <reified T> scanPaths(
        paths: List<String>,
        scanType: CliScanType,
        noinline cancelledCallback: TaskCancelledCallback = null
    ): CliResult<T>? {
        val scanTypeString = scanType.name.toLowerCase()
        val result = cli
            .executeCommand<T>(
                "scan",
                "-t",
                scanTypeString,
                "path",
                *getPathsAsArguments(paths),
                cancelledCallback = cancelledCallback
            )

        return processResult(result)
    }

    private fun getPathsAsArguments(paths: List<String>): Array<String> {
        return paths.toTypedArray()
    }

    fun scanPathsSecrets(
        paths: List<String>,
        onDemand: Boolean = true,
        cancelledCallback: TaskCancelledCallback = null
    ) {
        val results = scanPaths<SecretScanResult>(paths, CliScanType.Secret, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to scan paths: $paths")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Secret, detectionsCount, onDemand)

        // TODO(MarshalX): run only for the provided file?
        // save results and rerun annotators
        scanResults.setSecretResults(results)
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    fun scanPathsSca(paths: List<String>, onDemand: Boolean = true, cancelledCallback: TaskCancelledCallback = null) {
        val results = scanPaths<ScaScanResult>(paths, CliScanType.Sca, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to scan paths: $paths")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Sca, detectionsCount, onDemand)

        scanResults.setScaResults(results)
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
