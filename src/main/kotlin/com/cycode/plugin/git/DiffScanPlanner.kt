package com.cycode.plugin.git

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.DiffScanMode
import com.cycode.plugin.cli.ScanTarget
import com.cycode.plugin.cli.isSafeRefArg
import com.cycode.plugin.cli.supportsDiffScan

sealed class DiffScanPlan {
    data class Run(val target: ScanTarget) : DiffScanPlan()

    /** The scan must not start; [messageKey] is the bundle key of the message to show. */
    data class Refused(val messageKey: String) : DiffScanPlan()
}

/**
 * Decides what a diff scan of [scanType] should run, without touching the IDE. Runs git, so call it on a
 * background thread.
 */
fun planDiffScan(scanType: CliScanType, from: String, to: String, projectRoot: String, git: GitRepository): DiffScanPlan {
    if (from == to) {
        return DiffScanPlan.Refused("diffSameRevisionNotification")
    }

    // IaC does not support diffs in the CLI: run it as a normal project scan
    if (!supportsDiffScan(scanType)) {
        return DiffScanPlan.Run(ScanTarget.Paths(listOf(projectRoot)))
    }

    if (!git.isGitRepository()) {
        return DiffScanPlan.Refused("diffNotGitRepositoryNotification")
    }

    if (from == LOCAL_DIRECTORY_REF || to == LOCAL_DIRECTORY_REF) {
        val ref = if (from == LOCAL_DIRECTORY_REF) to else from
        if (!isSafeRefArg(ref)) {
            return DiffScanPlan.Refused("diffUnknownRevisionNotification")
        }
        return DiffScanPlan.Run(ScanTarget.Diff(DiffScanMode.LocalDiff, ref, projectRoot))
    }

    return when (val result = git.getCommitRange(from, to)) {
        is CommitRangeResult.Range -> DiffScanPlan.Run(ScanTarget.Diff(DiffScanMode.CommitRange, result.range, projectRoot))
        CommitRangeResult.SameCommit -> DiffScanPlan.Refused("diffSameCommitNotification")
        CommitRangeResult.NoCommonHistory -> DiffScanPlan.Refused("diffNoCommonHistoryNotification")
        CommitRangeResult.UnknownRevision -> DiffScanPlan.Refused("diffUnknownRevisionNotification")
        CommitRangeResult.NotARepository -> DiffScanPlan.Refused("diffNotGitRepositoryNotification")
        CommitRangeResult.GitFailure -> DiffScanPlan.Refused("diffGitFailureNotification")
    }
}
