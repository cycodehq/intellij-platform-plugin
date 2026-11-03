package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.*
import com.cycode.plugin.git.*
import com.cycode.plugin.cli.models.AiRemediationResultData
import com.cycode.plugin.components.toolWindow.CycodeToolWindowFactory
import com.cycode.plugin.components.toolWindow.ScanTabs
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.components.toolWindow.updateToolWindowStateForAllProjects
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicLong


@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) : Disposable {
    private val cliService = cli(project)
    private val cliDownloadService = cliDownload()

    private val pluginLocalState = pluginLocalState(project)
    private val pluginState = pluginState()
    private val refreshRequestId = AtomicLong()

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

    /** Background task for a scan that also marks the scan type as running (spinner in the scan tab). */
    private fun runScanBackgroundTask(scanType: CliScanType, task: (ProgressIndicator) -> Unit) {
        pluginLocalState.scanStarted(scanType)
        updateToolWindowState(project, reloadResults = false)
        runBackgroundTask(getBackgroundScanningLabel(scanType)) { indicator ->
            try {
                task(indicator)
            } finally {
                pluginLocalState.scanFinished(scanType)
                // the results (if any) were already reloaded by CliService; only the spinners change here
                updateToolWindowState(project, reloadResults = false)
            }
        }
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
            if (!pluginLocalState.cliAuthed) {
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
        runScanBackgroundTask(scanType) { indicator ->
            if (!pluginLocalState.cliAuthed) {
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("authorizationRequiredNotification"))
                return@runScanBackgroundTask
            }

            thisLogger().debug("[$scanType] Start scanning paths: $pathsToScan")
            cliService.scan(scanType, ScanTarget.Paths(pathsToScan), onDemand) { indicator.isCanceled }
            thisLogger().debug("[$scanType] Finish scanning paths: $pathsToScan")
        }
    }

    // region scan scope

    fun getScanScope(): ScanScope {
        return ScanScope.entries.firstOrNull { it.name == pluginState.scanScope } ?: ScanScope.Project
    }

    fun setScanScope(scope: ScanScope) {
        pluginState.scanScope = scope.name
        if (scope == ScanScope.Diff) {
            refreshDiffRefs()
        }
    }

    fun isDiffScanSupported(): Boolean = isDiffScanSupportedByCli(pluginLocalState.cliStatus?.version)

    /** Until the CLI status is known (or if the CLI is too old), Diff behaves as Project. */
    fun getEffectiveScanScope(): ScanScope {
        val scope = getScanScope()
        return if (scope == ScanScope.Diff && !isDiffScanSupported()) ScanScope.Project else scope
    }

    fun setDiffRefs(from: String, to: String) {
        pluginLocalState.diffFromRef = from
        pluginLocalState.diffToRef = to
    }

    /**
     * Reloads the revision lists in the background and resets stale picks to the defaults.
     * A request id drops results of superseded requests.
     */
    fun refreshDiffRefs(onDone: (() -> Unit)? = null) {
        val projectRoot = cliService.getProjectRootDirectory() ?: return
        val requestId = refreshRequestId.incrementAndGet()
        val pinned = listOf(pluginLocalState.diffFromRef, pluginLocalState.diffToRef)

        ApplicationManager.getApplication().executeOnPooledThread {
            val git = GitRepository(projectRoot)
            if (!git.isGitAvailable() && !pluginLocalState.gitUnavailableNotified) {
                pluginLocalState.gitUnavailableNotified = true
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("diffGitUnavailableNotification"))
            }

            val refs = git.getAvailableRefs(pinned)
            if (requestId != refreshRequestId.get() || project.isDisposed) {
                return@executeOnPooledThread
            }

            val values = refs.map { it.value }.toSet() + LOCAL_DIRECTORY_REF
            if (pluginLocalState.diffFromRef !in values) pluginLocalState.diffFromRef = LOCAL_DIRECTORY_REF
            if (pluginLocalState.diffToRef !in values) pluginLocalState.diffToRef = HEAD_REF
            pluginLocalState.diffRefs = refs

            if (onDone != null) {
                ApplicationManager.getApplication().invokeLater({ onDone() }, project.disposed)
            }
        }
    }

    /**
     * Scans according to the selected scope.
     */
    fun startScanForCurrentScope(scanType: CliScanType) {
        when (getEffectiveScanScope()) {
            ScanScope.Project -> startScanForCurrentProject(scanType)
            ScanScope.File -> startScanForCurrentFile(scanType)
            ScanScope.Diff -> startDiffScan(scanType)
        }
    }

    /** Runs every scan type enabled for the tenant, according to the selected scope. */
    fun startAllScansForCurrentScope() {
        // without an open file every scan type would show the same message: say it once
        if (getEffectiveScanScope() == ScanScope.File && currentLocalFilePath() == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noOpenFileErrorNotification"))
            return
        }

        val local = pluginLocalState
        val scanTypes = listOf(
            CliScanType.Secret to local.isSecretScanningEnabled,
            CliScanType.Sca to local.isScaScanningEnabled,
            CliScanType.Sast to local.isSastScanningEnabled,
            CliScanType.Iac to local.isIacScanningEnabled,
        ).filter { it.second }.map { it.first }

        if (getEffectiveScanScope() != ScanScope.File) {
            scanTypes.forEach { startScanForCurrentScope(it) }
            return
        }

        // one combined message for the types that cannot scan the open file, instead of one per type
        val filePath = currentLocalFilePath() ?: return
        val (supported, unsupported) = scanTypes.partition { isFileSupportedByScanType(filePath, it) }
        supported.forEach { startScan(it, listOf(filePath)) }
        if (unsupported.isNotEmpty()) {
            val names = unsupported.joinToString(", ") { getScanTypeDisplayName(it) }
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("scanFileUnsupportedNotification", names))
        }
    }

    private fun isFileSupportedByScanType(filePath: String, scanType: CliScanType): Boolean {
        return when (scanType) {
            CliScanType.Sca -> isSupportedPackageFile(filePath)
            CliScanType.Iac -> isSupportedIacFile(filePath)
            else -> true
        }
    }

    private fun currentLocalFilePath(): String? {
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        return file?.takeIf { it.isInLocalFileSystem }?.canonicalPath
    }

    private fun startScanForCurrentFile(scanType: CliScanType) {
        val filePath = currentLocalFilePath()
        if (filePath == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noOpenFileErrorNotification"))
            return
        }

        if (!isFileSupportedByScanType(filePath, scanType)) {
            CycodeNotifier.notifyInfo(
                project,
                CycodeBundle.message("scanFileUnsupportedNotification", getScanTypeDisplayName(scanType))
            )
            return
        }

        startScan(scanType, listOf(filePath))
    }

    private fun startDiffScan(scanType: CliScanType) {
        val from = pluginLocalState.diffFromRef
        val to = pluginLocalState.diffToRef

        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        runScanBackgroundTask(scanType) { indicator ->
            if (!pluginLocalState.cliAuthed) {
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("authorizationRequiredNotification"))
                return@runScanBackgroundTask
            }

            val target = when (val plan = planDiffScan(scanType, from, to, projectRoot, GitRepository(projectRoot))) {
                is DiffScanPlan.Run -> plan.target
                is DiffScanPlan.Refused -> {
                    CycodeNotifier.notifyInfo(project, CycodeBundle.message(plan.messageKey))
                    return@runScanBackgroundTask
                }
            }

            thisLogger().debug("[$scanType] Start diff scan: $target")
            cliService.scan(scanType, target, true) { indicator.isCanceled }
            thisLogger().debug("[$scanType] Finish diff scan: $target")
        }
    }

    // endregion

    fun getAiRemediation(detectionId: String, onSuccess: (AiRemediationResultData) -> Unit) {
        runBackgroundTask(CycodeBundle.message("aiRemediationGenerating")) { indicator ->
            thisLogger().debug("[AI REMEDIATION] Start generating remediation for $detectionId")
            val aiRemediation = cliService.getAiRemediation(detectionId)
            thisLogger().debug("[AI REMEDIATION] Finish generating remediation for $detectionId")

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
            CliIgnoreType.CVE -> "--by-cve"
        }
    }

    private fun applyIgnoreInUi(type: CliIgnoreType, value: String) {
        // exclude results from the local DB and restart the code analyzer

        val scanResults = scanResults(project)
        when (type) {
            CliIgnoreType.VALUE -> scanResults.excludeResults(byValue = value)
            CliIgnoreType.RULE -> scanResults.excludeResults(byRuleId = value)
            CliIgnoreType.PATH -> scanResults.excludeResults(byPath = value)
            CliIgnoreType.CVE -> scanResults.excludeResults(byCve = value)
        }

        DaemonCodeAnalyzer.getInstance(project).restart()
        updateToolWindowState(project)
    }

    fun applyIgnoreFromFileAnnotation(scanType: CliScanType, type: CliIgnoreType, value: String) {
        // we are removing is from UI first to show how it's blazing fast and then apply it in the background
        applyIgnoreInUi(type, value)

        runBackgroundTask(CycodeBundle.message("ignoresApplying"), canBeCancelled = false) { indicator ->
            if (!pluginLocalState.cliAuthed) {
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
        ScanTabs.remove(project)
        CycodeToolWindowFactory.TabManager.removeTab(project)
    }
}