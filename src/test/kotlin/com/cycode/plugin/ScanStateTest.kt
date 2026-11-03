package com.cycode.plugin

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.services.CycodeTemporaryStateService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Properties

class ScanStateTest {
    @Test
    fun overlappingScansOfOneTypeKeepItRunningUntilBothFinish() {
        val state = CycodeTemporaryStateService()
        state.scanStarted(CliScanType.Secret)
        state.scanStarted(CliScanType.Secret)
        assertTrue(state.isScanRunning(CliScanType.Secret))
        assertTrue(state.hasRunningScans())

        state.scanFinished(CliScanType.Secret)
        assertTrue(state.isScanRunning(CliScanType.Secret))

        state.scanFinished(CliScanType.Secret)
        assertFalse(state.isScanRunning(CliScanType.Secret))
        assertFalse(state.hasRunningScans())
    }

    @Test
    fun scanTypesAreTrackedIndependentlyAndNeverGoNegative() {
        val state = CycodeTemporaryStateService()
        state.scanFinished(CliScanType.Sca)
        state.scanStarted(CliScanType.Sast)
        assertFalse(state.isScanRunning(CliScanType.Sca))
        assertTrue(state.isScanRunning(CliScanType.Sast))

        state.scanStarted(CliScanType.Sca)
        assertTrue(state.isScanRunning(CliScanType.Sca))
    }

    @Test
    fun messagesUsedByTheScanScopeFeatureExist() {
        val bundle = Properties()
        javaClass.getResourceAsStream("/messages/CycodeBundle.properties")!!.use { bundle.load(it.reader(Charsets.UTF_8)) }

        val keys = listOf(
            "diffSameRevisionNotification", "diffSameCommitNotification", "diffNoCommonHistoryNotification",
            "diffUnknownRevisionNotification", "diffNotGitRepositoryNotification", "diffGitFailureNotification",
            "diffGitUnavailableNotification", "scanFileUnsupportedNotification", "noOpenFileErrorNotification",
            "scanScopeProject", "scanScopeDiff", "scanScopeFile", "scanScopeProjectHint", "scanScopeDiffHint",
            "scanScopeFileHint", "scanScopeDiffIacNote", "scanScopeDiffNoGitNote", "scanScopeDiffFromLabel", "scanScopeDiffToLabel",
            "scanScopeDiffLocalDirectory", "scanTabHeader", "scanTabHeaderCollapsed", "scanTabScanAllBtn",
            "scanTabCollapse", "scanTabExpand", "violationCardCloseTooltip",
        )
        keys.forEach { assertTrue("Missing message key: $it", bundle.containsKey(it)) }
    }
}
