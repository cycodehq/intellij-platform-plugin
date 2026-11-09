package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.ScanScope
import com.cycode.plugin.cli.ScanTarget
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getScanTypeDisplayName
import com.cycode.plugin.cli.isDiffScanSupportedByCli
import com.cycode.plugin.cli.isFileSupportedByScanType
import com.cycode.plugin.cli.partitionByFileSupport
import com.cycode.plugin.git.DiffScanPlan
import com.cycode.plugin.git.GitCli
import com.cycode.plugin.git.planDiffScan
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

/**
 * The scan scope (Project / Diff / File): which scope is selected and starting scans for it. The git side of Diff
 * (revisions, repository state) lives in [DiffRefsService].
 */
@Service(Service.Level.PROJECT)
class ScanScopeService(private val project: Project) {
    private val cliService = cli(project)
    private val cycodeService = cycode(project)
    private val pluginLocalState = pluginLocalState(project)
    private val projectState = projectState(project)
    private val diffRefs = diffRefs(project)

    fun getScanScope(): ScanScope {
        return ScanScope.entries.firstOrNull { it.name == projectState.scanScope } ?: ScanScope.Project
    }

    fun setScanScope(scope: ScanScope) {
        projectState.scanScope = scope.name
        if (scope == ScanScope.Diff) {
            diffRefs.refresh()
        }
    }

    fun isDiffScanSupported(): Boolean = isDiffScanSupportedByCli(pluginLocalState.cliStatus?.version)

    /** Diff needs a new enough CLI and a git repository. */
    fun isDiffScanAvailable(): Boolean = isDiffScanSupported() && diffRefs.isGitRepository != false

    /** Until the CLI status is known (or if the CLI is too old or there is no repository), Diff behaves as Project. */
    fun getEffectiveScanScope(): ScanScope {
        val scope = getScanScope()
        return if (scope == ScanScope.Diff && !isDiffScanAvailable()) ScanScope.Project else scope
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
        val (supported, unsupported) = partitionByFileSupport(filePath, scanTypes)
        supported.forEach { cycodeService.startScan(it, listOf(filePath)) }
        if (unsupported.isNotEmpty()) {
            val names = unsupported.joinToString(", ") { getScanTypeDisplayName(it) }
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("scanFileUnsupportedNotification", names))
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
        val from = diffRefs.from
        val to = diffRefs.to

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

            val target = when (val plan = planDiffScan(scanType, from, to, projectRoot, GitCli(projectRoot))) {
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
