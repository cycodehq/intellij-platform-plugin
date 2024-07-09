package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.*
import com.cycode.plugin.cli.models.AuthCheckResult
import com.cycode.plugin.cli.models.AuthResult
import com.cycode.plugin.cli.models.VersionResult
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.iac.IacScanResult
import com.cycode.plugin.cli.models.scanResult.sast.SastScanResult
import com.cycode.plugin.cli.models.scanResult.sca.ScaScanResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.sentry.SentryInit
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

    private val scanResults = scanResults(project)

    private val cli = CliWrapper(pluginSettings.cliPath, getProjectRootDirectory())

    fun getProjectRootDirectory(): String? {
        val modules = ModuleManager.getInstance(project).modules
        if (modules.isEmpty()) {
            return null
        }

        val module = modules[0]
        return module.project.basePath
    }

    private fun rerunAnnotators() {
        // TODO(MarshalX): run only for the provided file?
        DaemonCodeAnalyzer.getInstance(project).restart()
        updateToolWindowState(project)
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

        if (result is CliResult.Success && result.result is ScanResultBase) {
            val errors = result.result.errors
            if (errors.isEmpty()) {
                return result
            }

            for (error in errors) {
                showErrorNotification(error.message)
            }

            // we trust that it is not possible to have both errors and detections
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

            SentryInit.setupScope(
                processedResult.result.data.userId,
                processedResult.result.data.tenantId,
            )

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

    private fun getCliScanOptions(scanType: CliScanType): Array<String> {
        val options = mutableListOf<String>()
        if (scanType == CliScanType.Sca && pluginSettings.scaSyncFlow) {
            options.add("--sync")
            options.add("--no-restore")
        }
        return options.toTypedArray()
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
                *getCliScanOptions(scanType),
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

        scanResults.setSecretResults(results)
        rerunAnnotators()
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
        rerunAnnotators()
    }

    private fun filterUnsupportedIacDetections(detections: List<IacDetection>): List<IacDetection> {
        return detections.filter { detection ->
            val detectionDetails = detection.detectionDetails
            val filePath = detectionDetails.getFilepath()

            // TF plans are virtual files what is not exist in the file system
            // "file_name": "1711298252-/Users/ilyasiamionau/projects/cycode/ilya-siamionau-payloads/tfplan.tf",
            // skip such detections
            return@filter filePath.startsWith("/")
        }
    }

    fun scanPathsIac(paths: List<String>, onDemand: Boolean = true, cancelledCallback: TaskCancelledCallback = null) {
        var results = scanPaths<IacScanResult>(paths, CliScanType.Iac, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to scan paths: $paths")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            // filter unsupported detections
            results = CliResult.Success(
                IacScanResult(
                    filterUnsupportedIacDetections(results.result.detections),
                    results.result.errors
                )
            )

            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Iac, detectionsCount, onDemand)

        scanResults.setIacResults(results)
        rerunAnnotators()
    }

    fun scanPathsSast(paths: List<String>, onDemand: Boolean = true, cancelledCallback: TaskCancelledCallback = null) {
        val results = scanPaths<SastScanResult>(paths, CliScanType.Sast, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to scan paths: $paths")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Sast, detectionsCount, onDemand)

        scanResults.setSastResults(results)
        rerunAnnotators()
    }
}
