package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.ScanScope
import com.cycode.plugin.cli.ScanTarget
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getScanTypeDisplayName
import com.cycode.plugin.cli.isDiffScanSupportedByCli
import com.cycode.plugin.cli.isSupportedIacFile
import com.cycode.plugin.cli.isSupportedPackageFile
import com.cycode.plugin.git.DiffScanPlan
import com.cycode.plugin.git.GitRepository
import com.cycode.plugin.git.HEAD_REF
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.planDiffScan
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicLong

/**
 * The scan scope (Project / Diff / File): which scope is selected, the revisions of a diff scan and starting
 * scans for the selected scope.
 */
@Service(Service.Level.PROJECT)
class ScanScopeService(private val project: Project) {
    private val cliService = cli(project)
    private val cycodeService = cycode(project)
    private val pluginLocalState = pluginLocalState(project)
    private val pluginState = pluginState()
    private val refreshRequestId = AtomicLong()

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

            // the lists are rebuilt only when the branch changes or a commit is created
            val fingerprint = git.getHeadFingerprint()
            if (fingerprint == null && !pluginLocalState.gitUnavailableNotified && !git.isGitAvailable()) {
                pluginLocalState.gitUnavailableNotified = true
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("diffGitUnavailableNotification"))
            }

            val cached = pluginLocalState.diffRefs
            val refs = if (fingerprint != null && fingerprint == pluginLocalState.diffRefsFingerprint && cached.isNotEmpty()) {
                cached
            } else {
                git.getAvailableRefs(pinned).also { pluginLocalState.diffRefsFingerprint = fingerprint }
            }
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
            ScanScope.Project -> cycodeService.startScanForCurrentProject(scanType)
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
        supported.forEach { cycodeService.startScan(it, listOf(filePath)) }
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

        cycodeService.startScan(scanType, listOf(filePath))
    }

    private fun startDiffScan(scanType: CliScanType) {
        val from = pluginLocalState.diffFromRef
        val to = pluginLocalState.diffToRef

        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        cycodeService.runScanBackgroundTask(scanType) { indicator ->
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
}
