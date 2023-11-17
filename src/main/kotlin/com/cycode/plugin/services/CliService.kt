package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.*
import com.cycode.plugin.cli.models.AuthCheckResult
import com.cycode.plugin.cli.models.AuthResult
import com.cycode.plugin.cli.models.VersionResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project


@Service(Service.Level.PROJECT)
class CliService(private val project: Project) {
    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()

    private val scanResults = scanResults()

    private val cli = CliWrapper(pluginSettings.cliPath, getCliWorkingDirectory())

    var cliShouldDestroyCallback: (() -> Boolean)? = null

    private fun getCliWorkingDirectory(): String? {
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

    fun healthCheck(): Boolean {
        val result: CliResult<VersionResult> =
            cli.executeCommand(
                "version",
                shouldDestroyCallback = cliShouldDestroyCallback
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

    fun checkAuth(): Boolean {
        val result: CliResult<AuthCheckResult> =
            cli.executeCommand(
                "auth",
                "check",
                shouldDestroyCallback = cliShouldDestroyCallback
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

    fun doAuth(): Boolean {
        val result: CliResult<AuthResult> =
            cli.executeCommand(
                "auth",
                shouldDestroyCallback = cliShouldDestroyCallback
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

    fun ignore(optionName: String, optionValue: String): Boolean {
        val result: CliResult<Unit> = cli.executeCommand(
            "ignore",
            optionName,
            optionValue,
            shouldDestroyCallback = cliShouldDestroyCallback
        )

        val processedResult = processResult(result)
        return processedResult is CliResult.Success
    }

    private fun showScanFileResultNotification(scanType: CliScanType, detectionsCount: Int) {
        if (detectionsCount < 1) {
            return
        }

        val scanTypeName = getScanTypeDisplayName(scanType)
        val message = CycodeBundle.message("scanFileResultNotification", detectionsCount, scanTypeName)
        CycodeNotifier.notifyDetections(project, message)
    }

    private inline fun <reified T> scanFile(filePath: String, scanType: CliScanType): CliResult<T>? {
        val scanTypeString = scanType.name.toLowerCase()
        val result = cli
            .executeCommand<T>(
                "scan",
                "-t",
                scanTypeString,
                "path",
                filePath,
                shouldDestroyCallback = cliShouldDestroyCallback
            )

        return processResult(result)
    }

    fun scanFileSecrets(filePath: String) {
        val results = scanFile<SecretScanResult>(filePath, CliScanType.Secret)
        if (results == null) {
            thisLogger().warn("Failed to scan file: $filePath")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Secret, detectionsCount)

        // TODO(MarshalX): run only for the provided file?
        // save results and rerun annotators
        scanResults.setSecretsResults(results)
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
