package com.cycode.plugin

import com.cycode.plugin.git.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Uses real temporary git repositories: the exit-code semantics are the point. */
class GitRepositoryTest {
    private lateinit var dir: File
    private lateinit var git: GitRepository

    private fun sh(vararg args: String): String {
        val process = ProcessBuilder(listOf("git", "-c", "user.name=t", "-c", "user.email=t@t", *args))
            .directory(dir).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0) { "git ${args.toList()} failed: $out" }
        return out
    }

    private fun commit(name: String) {
        File(dir, name).writeText(name)
        sh("add", name)
        sh("commit", "-m", name)
    }

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("cycode-git").toFile()
        sh("init", "-b", "main")
        commit("base")
        git = GitRepository(dir.path)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun diverge() {
        sh("checkout", "-b", "feature")
        commit("feature-1")
        sh("checkout", "main")
        commit("main-1")
    }

    @Test
    fun divergedBranchesGiveMergeBaseRangeBothWays() {
        diverge()
        val base = sh("merge-base", "main", "feature")
        val feature = sh("rev-parse", "feature")
        val main = sh("rev-parse", "main")

        assertEquals(CommitRangeResult.Range("$base..$feature"), git.getCommitRange("main", "feature"))
        assertEquals(CommitRangeResult.Range("$base..$main"), git.getCommitRange("feature", "main"))
    }

    @Test
    fun newestFirstIsSwapped() {
        val old = sh("rev-parse", "HEAD")
        commit("second")
        val new = sh("rev-parse", "HEAD")

        assertEquals(CommitRangeResult.Range("$old..$new"), git.getCommitRange(old, new))
        assertEquals(CommitRangeResult.Range("$old..$new"), git.getCommitRange(new, old))
    }

    @Test
    fun sameCommit() {
        assertEquals(CommitRangeResult.SameCommit, git.getCommitRange("HEAD", "main"))
    }

    @Test
    fun noCommonHistory() {
        sh("checkout", "--orphan", "orphan")
        commit("orphan-1")
        assertEquals(CommitRangeResult.NoCommonHistory, git.getCommitRange("main", "orphan"))
    }

    @Test
    fun unknownAndOptionLikeRevisions() {
        assertEquals(CommitRangeResult.UnknownRevision, git.getCommitRange("main", "nope"))
        assertEquals(CommitRangeResult.UnknownRevision, git.getCommitRange("main", "--output=x"))
    }

    @Test
    fun nonGitFolder() {
        val plain = Files.createTempDirectory("cycode-plain").toFile()
        try {
            assertFalse(GitRepository(plain.path).isGitRepository())
            assertTrue(git.isGitRepository())
        } finally {
            plain.deleteRecursively()
        }
    }

    @Test
    fun refLabelsAndPinning() {
        diverge()
        val refs = git.getAvailableRefs()
        assertEquals(HEAD_REF, refs.first().value)
        assertTrue(refs.any { it.value == "feature" && it.label.startsWith("feature$LABEL_SEPARATOR") })
        // a commit that is a branch tip is listed once, under the ref name
        assertEquals(1, refs.count { it.label.endsWith("main-1") })

        val first = sh("rev-list", "--max-parents=0", "HEAD")
        assertTrue(git.getAvailableRefs(listOf(first)).any { it.value == first })
        assertFalse(git.getAvailableRefs(listOf("nope")).any { it.value == "nope" })
    }

    @Test
    fun labelFormat() {
        assertEquals("main │ abc123 │ subject", GitRepository.buildLabel("main", "abc123", "subject"))
    }
}

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

    private fun plan(type: com.cycode.plugin.cli.CliScanType, from: String, to: String, repo: GitRepository = git) =
        planDiffScan(type, from, to, dir.path, repo)

    private val secret = com.cycode.plugin.cli.CliScanType.Secret

    @Test
    fun localDiffInBothOrders() {
        val expected = DiffScanPlan.Run(
            com.cycode.plugin.cli.ScanTarget.Diff(com.cycode.plugin.cli.DiffScanMode.LocalDiff, "main", dir.path)
        )
        assertEquals(expected, plan(secret, LOCAL_DIRECTORY_REF, "main"))
        assertEquals(expected, plan(secret, "main", LOCAL_DIRECTORY_REF))
    }

    @Test
    fun commitRangeIsResolved() {
        val plan = plan(secret, "other", "main") as DiffScanPlan.Run
        val target = plan.target as com.cycode.plugin.cli.ScanTarget.Diff
        assertEquals(com.cycode.plugin.cli.DiffScanMode.CommitRange, target.mode)
        assertTrue(target.ref.matches(Regex("[0-9a-f]{40}\\.\\.[0-9a-f]{40}")))
    }

    @Test
    fun iacFallsBackToFullScanEvenOutsideGit() {
        val plain = Files.createTempDirectory("cycode-plain").toFile()
        try {
            val expected = DiffScanPlan.Run(com.cycode.plugin.cli.ScanTarget.Paths(listOf(dir.path)))
            val iac = com.cycode.plugin.cli.CliScanType.Iac
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
