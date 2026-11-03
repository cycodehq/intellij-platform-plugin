package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.*
import com.cycode.plugin.cli.models.AiRemediationResult
import com.cycode.plugin.cli.models.AiRemediationResultData
import com.cycode.plugin.cli.models.AuthResult
import com.cycode.plugin.cli.models.StatusResult
import com.cycode.plugin.cli.models.scanResult.ScanResultBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.iac.IacScanResult
import com.cycode.plugin.cli.models.scanResult.sast.SastScanResult
import com.cycode.plugin.cli.models.scanResult.sca.ScaScanResult
import com.cycode.plugin.cli.models.scanResult.secret.SecretScanResult
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.utils.CycodeNotifier
import com.cycode.plugin.utils.isValidExistedFilePath
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project


typealias TaskCancelledCallback = (() -> Boolean)?

@Service(Service.Level.PROJECT)
class CliService(private val project: Project) {
    private val pluginState = pluginState()
    private val pluginLocalState = pluginLocalState(project)
    private val scanResults = scanResults(project)
    private val cli = CliWrapper(getProjectRootDirectory())

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
        pluginLocalState.cliAuthed = false
        pluginLocalState.cliInstalled = false
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

    fun syncStatus(cancelledCallback: TaskCancelledCallback = null) {
        val result: CliResult<StatusResult> =
            cli.executeCommand(
                "status",
                cancelledCallback = cancelledCallback
            )

        val processedResult = processResult(result)
        if (processedResult !is CliResult.Success) {
            resetPluginCLiState()
            return
        }

        pluginLocalState.cliInstalled = true
        pluginLocalState.cliAuthed = processedResult.result.isAuthenticated
        pluginLocalState.cliStatus = processedResult.result
        pluginState.cliVer = processedResult.result.version

        if (!isDiffScanSupportedByCli(processedResult.result.version) &&
            pluginState.scanScope == ScanScope.Diff.name
        ) {
            pluginState.scanScope = ScanScope.Project.name
        }

        if (!pluginLocalState.cliAuthed) {
            showErrorNotification(CycodeBundle.message("checkAuthErrorNotification"))
        }

        return
    }

    fun startAuth(cancelledCallback: TaskCancelledCallback = null): Boolean {
        val result: CliResult<AuthResult> =
            cli.executeCommand(
                "auth",
                cancelledCallback = cancelledCallback
            )

        val processedResult = processResult(result)
        if (processedResult is CliResult.Success) {
            pluginLocalState.cliAuthed = processedResult.result.result
            if (!pluginLocalState.cliAuthed) {
                showErrorNotification(CycodeBundle.message("authErrorNotification"))
            }
            return pluginLocalState.cliAuthed
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

        if (scanType != CliScanType.Sast) {
            options.add("--sync")
        }

        if (scanType == CliScanType.Sca) {
            options.add("--no-restore")
        }

        return options.toTypedArray()
    }

    private inline fun <reified T> runScan(
        target: ScanTarget,
        scanType: CliScanType,
        noinline cancelledCallback: TaskCancelledCallback = null
    ): CliResult<T>? {
        val scanTypeString = scanType.name.lowercase()
        val result = cli
            .executeCommand<T>(
                "scan",
                "-t",
                scanTypeString,
                *getCliScanOptions(scanType),
                *target.toCliArgs(),
                cancelledCallback = cancelledCallback
            )

        return processResult(result)
    }

    /**
     * One runner for every kind of scan (paths, commit range, local diff).
     * Do not add one method per mode.
     */
    fun scan(
        scanType: CliScanType,
        target: ScanTarget,
        onDemand: Boolean = true,
        cancelledCallback: TaskCancelledCallback = null
    ) {
        when (scanType) {
            CliScanType.Secret -> scanSecrets(target, onDemand, cancelledCallback)
            CliScanType.Sca -> scanSca(target, onDemand, cancelledCallback)
            CliScanType.Iac -> scanIac(target, onDemand, cancelledCallback)
            CliScanType.Sast -> scanSast(target, onDemand, cancelledCallback)
        }
    }

    private fun scanSecrets(target: ScanTarget, onDemand: Boolean, cancelledCallback: TaskCancelledCallback) {
        val results = runScan<SecretScanResult>(target, CliScanType.Secret, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to scan Secret: $target")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Secret, detectionsCount, onDemand)

        scanResults.secretResults = results
        rerunAnnotators()
    }

    private fun scanSca(target: ScanTarget, onDemand: Boolean, cancelledCallback: TaskCancelledCallback) {
        val results = runScan<ScaScanResult>(target, CliScanType.Sca, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to scan SCA: $target")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Sca, detectionsCount, onDemand)

        scanResults.scaResults = results
        rerunAnnotators()
    }

    private fun filterUnsupportedIacDetections(detections: List<IacDetection>): List<IacDetection> {
        return detections.filter { detection ->
            val detectionDetails = detection.detectionDetails
            val filePath = detectionDetails.getFilepath()

            // TF plans are virtual files what is not exist in the file system
            // "file_name": "1711298252-/Users/ilyasiamionau/projects/cycode/ilya-siamionau-payloads/tfplan.tf",
            // skip such detections
            return@filter isValidExistedFilePath(filePath)
        }
    }

    private fun scanIac(target: ScanTarget, onDemand: Boolean, cancelledCallback: TaskCancelledCallback) {
        var results = runScan<IacScanResult>(target, CliScanType.Iac, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to IaC scan: $target")
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

        scanResults.iacResults = results
        rerunAnnotators()
    }

    private fun scanSast(target: ScanTarget, onDemand: Boolean, cancelledCallback: TaskCancelledCallback) {
        val results = runScan<SastScanResult>(target, CliScanType.Sast, cancelledCallback)
        if (results == null) {
            thisLogger().warn("Failed to SAST scan: $target")
            return
        }

        var detectionsCount = 0
        if (results is CliResult.Success) {
            detectionsCount = results.result.detections.count()
        }

        showScanFileResultNotification(CliScanType.Sast, detectionsCount, onDemand)

        scanResults.sastResults = results
        rerunAnnotators()
    }

    fun getAiRemediation(
        detectionId: String,
        cancelledCallback: TaskCancelledCallback = null
    ): AiRemediationResultData? {
        val result: CliResult<AiRemediationResult> =
            cli.executeCommand(
                "ai_remediation",
                detectionId,
                cancelledCallback = cancelledCallback
            )

        val processedResult = processResult(result)
        if (processedResult !is CliResult.Success) {
            thisLogger().warn("Failed to get AI remediation for detection: $detectionId")
            return null
        }

        if (!processedResult.result.result || processedResult.result.data?.remediation == null) {
            thisLogger().warn("AI remediation is not available for detection: $detectionId")
            showErrorNotification(CycodeBundle.message("aiRemediationNotAvailableNotification"))
            return null
        }

        return processedResult.result.data
    }
}
