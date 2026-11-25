package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.ScanScope
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getScanTypeDisplayName
import com.cycode.plugin.cli.partitionByFileSupport
import com.cycode.plugin.git.DiffScanPlan
import com.cycode.plugin.git.GitCli
import com.cycode.plugin.git.planDiffScan
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.openapi.components.Service
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

    /** Diff needs a git repository. */
    fun isDiffScanAvailable(): Boolean = diffRefs.isGitRepository != false

    /** Without a git repository Diff behaves as Project. */
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
            ScanScope.File -> startFileScan(listOf(scanType))
            ScanScope.Diff -> startDiffScan(scanType)
        }
    }

    /** Runs every scan type enabled for the tenant, according to the selected scope. */
    fun startAllScansForCurrentScope() {
        val scanTypes = pluginLocalState.enabledScanTypes
        when (getEffectiveScanScope()) {
            ScanScope.File -> startFileScan(scanTypes)
            else -> scanTypes.forEach { startScanForCurrentScope(it) }
        }
    }

    private fun currentLocalFilePath(): String? {
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        return file?.takeIf { it.isInLocalFileSystem }?.canonicalPath
    }

    /** Scans the open file with the types that support it, with one combined message for the rest. */
    private fun startFileScan(scanTypes: List<CliScanType>) {
        val filePath = currentLocalFilePath()
        if (filePath == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noOpenFileErrorNotification"))
            return
        }

        val (supported, unsupported) = partitionByFileSupport(filePath, scanTypes)
        supported.forEach { cycodeService.startScan(it, listOf(filePath)) }
        if (unsupported.isNotEmpty()) {
            val names = unsupported.joinToString(", ") { getScanTypeDisplayName(it) }
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("scanFileUnsupportedNotification", names))
        }
    }

    private fun startDiffScan(scanType: CliScanType) {
        val from = diffRefs.from
        val to = diffRefs.to

        val projectRoot = cliService.getProjectRootDirectory()
        if (projectRoot == null) {
            CycodeNotifier.notifyInfo(project, CycodeBundle.message("noProjectRootErrorNotification"))
            return
        }

        cycodeService.startScan(scanType) {
            when (val plan = planDiffScan(scanType, from, to, projectRoot, GitCli(projectRoot))) {
                is DiffScanPlan.Run -> plan.target
                is DiffScanPlan.Refused -> {
                    CycodeNotifier.notifyInfo(project, CycodeBundle.message(plan.messageKey))
                    null
                }
            }
        }
    }
}
