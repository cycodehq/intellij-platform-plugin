package com.cycode.plugin

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.DiffScanMode
import com.cycode.plugin.cli.ScanTarget
import com.cycode.plugin.git.DiffScanPlan
import com.cycode.plugin.git.GitRepository
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.planDiffScan
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DiffScanPlannerTest {
    private lateinit var dir: File
    private lateinit var git: GitRepository

    private fun sh(vararg args: String) {
        val process = ProcessBuilder(listOf("git", "-c", "user.name=t", "-c", "user.email=t@t", *args))
            .directory(dir).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "git ${args.toList()} failed: $out" }
    }

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("cycode-plan").toFile()
        sh("init", "-b", "main")
        File(dir, "a").writeText("a")
        sh("add", "a")
        sh("commit", "-m", "a")
        sh("branch", "other")
        File(dir, "b").writeText("b")
        sh("add", "b")
        sh("commit", "-m", "b")
        git = GitRepository(dir.path)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
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
        val plain = Files.createTempDirectory("cycode-plain").toFile()
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

        val plain = Files.createTempDirectory("cycode-plain").toFile()
        try {
            assertEquals(
                DiffScanPlan.Refused("diffNotGitRepositoryNotification"),
                plan(secret, LOCAL_DIRECTORY_REF, "main", GitRepository(plain.path))
            )
        } finally {
            plain.deleteRecursively()
        }
    }
}
