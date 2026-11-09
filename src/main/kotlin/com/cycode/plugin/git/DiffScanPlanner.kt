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
fun planDiffScan(scanType: CliScanType, from: DiffSide, to: DiffSide, projectRoot: String, git: GitCli): DiffScanPlan {
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

    if (from is DiffSide.Local || to is DiffSide.Local) {
        val rev = (if (from is DiffSide.Local) to else from) as DiffSide.Rev
        if (!isSafeRefArg(rev.ref)) {
            return DiffScanPlan.Refused("diffUnknownRevisionNotification")
        }
        return DiffScanPlan.Run(ScanTarget.Diff(DiffScanMode.LocalDiff, rev.ref, projectRoot))
    }

    // both sides are revisions here (two local sides were caught by the equality check above)
    from as DiffSide.Rev
    to as DiffSide.Rev
    return when (val result = git.getCommitRange(from.ref, to.ref)) {
        is CommitRangeResult.Range -> DiffScanPlan.Run(ScanTarget.Diff(DiffScanMode.CommitRange, result.range, projectRoot))
        CommitRangeResult.SameCommit -> DiffScanPlan.Refused("diffSameCommitNotification")
        CommitRangeResult.NoCommonHistory -> DiffScanPlan.Refused("diffNoCommonHistoryNotification")
        CommitRangeResult.UnknownRevision -> DiffScanPlan.Refused("diffUnknownRevisionNotification")
        CommitRangeResult.NotARepository -> DiffScanPlan.Refused("diffNotGitRepositoryNotification")
        CommitRangeResult.GitFailure -> DiffScanPlan.Refused("diffGitFailureNotification")
    }
}
