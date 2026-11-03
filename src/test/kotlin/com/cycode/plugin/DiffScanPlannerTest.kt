package com.cycode.plugin

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.DiffScanMode
import com.cycode.plugin.cli.ScanTarget
import com.cycode.plugin.git.DiffScanPlan
import com.cycode.plugin.git.GitRepository
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.planDiffScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DiffScanPlannerTest : GitTestBase() {
    @Before
    fun addBranchAndCommit() {
        sh("branch", "other")
        commit("b")
    }

    private fun plan(type: CliScanType, from: String, to: String, repo: GitRepository = git) =
        planDiffScan(type, from, to, dir.path, repo)

    private val secret = CliScanType.Secret

    @Test
    fun localDiffInBothOrders() {
        val expected = DiffScanPlan.Run(
            ScanTarget.Diff(DiffScanMode.LocalDiff, "main", dir.path)
        )
        assertEquals(expected, plan(secret, LOCAL_DIRECTORY_REF, "main"))
        assertEquals(expected, plan(secret, "main", LOCAL_DIRECTORY_REF))
    }

    @Test
    fun commitRangeIsResolved() {
        val plan = plan(secret, "other", "main") as DiffScanPlan.Run
        val target = plan.target as ScanTarget.Diff
        assertEquals(DiffScanMode.CommitRange, target.mode)
        assertTrue(target.ref.matches(Regex("[0-9a-f]{40}\\.\\.[0-9a-f]{40}")))
    }

    @Test
    fun iacFallsBackToFullScanEvenOutsideGit() {
        val plain = createPlainDirectory()
        try {
            val expected = DiffScanPlan.Run(ScanTarget.Paths(listOf(dir.path)))
            val iac = CliScanType.Iac
            assertEquals(expected, plan(iac, LOCAL_DIRECTORY_REF, "main"))
            assertEquals(expected, plan(iac, "other", "main", GitRepository(plain.path)))
        } finally {
            plain.deleteRecursively()
        }
    }

    @Test
    fun sameRevisionIsRefused() {
        assertEquals(DiffScanPlan.Refused("diffSameRevisionNotification"), plan(secret, "main", "main"))
        assertEquals(
            DiffScanPlan.Refused("diffSameRevisionNotification"),
            plan(secret, LOCAL_DIRECTORY_REF, LOCAL_DIRECTORY_REF)
        )
    }

    @Test
    fun gitProblemsAreReportedAndStartNoScan() {
        assertEquals(DiffScanPlan.Refused("diffUnknownRevisionNotification"), plan(secret, "main", "nope"))

        val plain = createPlainDirectory()
        try {
            assertEquals(
                DiffScanPlan.Refused("diffNotGitRepositoryNotification"),
                plan(secret, LOCAL_DIRECTORY_REF, "main", GitRepository(plain.path))
            )
        } finally {
            plain.deleteRecursively()
        }
    }

    @Test
    fun unsafeRefsAreRefusedInBothFlavours() {
        assertEquals(
            DiffScanPlan.Refused("diffUnknownRevisionNotification"),
            plan(secret, LOCAL_DIRECTORY_REF, "--output=x")
        )
        assertEquals(DiffScanPlan.Refused("diffUnknownRevisionNotification"), plan(secret, "--output=x", "main"))
    }

    @Test
    fun sameCommitUnderDifferentNamesIsRefused() {
        assertEquals(DiffScanPlan.Refused("diffSameCommitNotification"), plan(secret, "main", "HEAD"))
    }

    @Test
    fun noCommonHistoryIsRefused() {
        sh("checkout", "--orphan", "orphan")
        File(dir, "o").writeText("o")
        sh("add", "o")
        sh("commit", "-m", "o")
        assertEquals(DiffScanPlan.Refused("diffNoCommonHistoryNotification"), plan(secret, "main", "orphan"))
    }

    @Test
    fun everyDiffScanTypeGetsAPlan() {
        CliScanType.entries.forEach {
            assertTrue(plan(it, LOCAL_DIRECTORY_REF, "main") is DiffScanPlan.Run)
        }
    }
}
