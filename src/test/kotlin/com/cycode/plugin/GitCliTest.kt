package com.cycode.plugin

import com.cycode.plugin.git.*
import org.junit.Assert.*
import org.junit.Test

/** Uses real temporary git repositories: the exit-code semantics are the point. */
class GitCliTest : GitTestBase() {
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
    fun gitIsAvailable() {
        assertTrue(git.isGitAvailable())
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
        val plain = createPlainDirectory()
        try {
            assertFalse(GitCli(plain.path).isGitRepository())
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
        assertEquals("main │ abc123 │ subject", GitCli.buildLabel("main", "abc123", "subject"))
    }

    @Test
    fun refListsAreCapped() {
        repeat(GitCli.MAX_REFS + 5) { sh("branch", "b$it") }
        repeat(GitCli.MAX_COMMITS + 5) { commit("c$it") }

        val refs = git.getAvailableRefs()
        val sha = Regex("[0-9a-f]{40}")
        val branchesAndTags = refs.count { it.value != HEAD_REF && !sha.matches(it.value) }
        assertTrue(branchesAndTags <= GitCli.MAX_REFS)
        // HEAD + capped refs + capped commits
        assertTrue(refs.size <= 1 + GitCli.MAX_REFS + GitCli.MAX_COMMITS)
    }

    @Test
    fun tagsAreListed() {
        sh("tag", "v1")
        assertTrue(git.getAvailableRefs().any { it.value == "v1" })
    }

    @Test
    fun commitsAreLabelledWithTheCurrentBranch() {
        commit("second")
        val commitOption = git.getAvailableRefs().first { it.label.endsWith("base") }
        assertTrue(commitOption.label.startsWith("main$LABEL_SEPARATOR"))
    }

    @Test
    fun detachedHeadCommitsHaveNoBranchPrefix() {
        commit("second")
        sh("checkout", "--detach", "HEAD~1")
        val refs = git.getAvailableRefs()
        val baseCommit = refs.first { it.label.endsWith("base") && it.value.length == 40 }
        assertFalse(baseCommit.label.contains("main"))
        assertEquals(HEAD_REF, refs.first().value)
    }

    @Test
    fun remoteHeadSymbolicRefIsSkipped() {
        sh("update-ref", "refs/remotes/origin/main", "HEAD")
        sh("symbolic-ref", "refs/remotes/origin/HEAD", "refs/remotes/origin/main")
        val refs = git.getAvailableRefs()
        assertTrue(refs.any { it.value == "origin/main" })
        assertFalse(refs.any { it.value.endsWith("/HEAD") })
    }

    @Test
    fun commitRangeWithTagsAndRelativeRevisions() {
        sh("tag", "v1")
        commit("second")
        val result = git.getCommitRange("v1", "HEAD") as CommitRangeResult.Range
        assertTrue(result.range.matches(Regex("[0-9a-f]{40}\\.\\.[0-9a-f]{40}")))
        assertEquals(result, git.getCommitRange("v1", "main"))
    }

    @Test
    fun unknownOrUnsafeFromRevision() {
        assertEquals(CommitRangeResult.UnknownRevision, git.getCommitRange("nope", "main"))
        assertEquals(CommitRangeResult.UnknownRevision, git.getCommitRange("--output=x", "main"))
        assertEquals(CommitRangeResult.UnknownRevision, git.getCommitRange("", "main"))
    }

    @Test
    fun pinnedRefThatScrolledOutOfTheListIsKept() {
        val oldest = sh("rev-parse", "HEAD")
        repeat(GitCli.MAX_COMMITS + 5) { commit("c$it") }
        val refs = git.getAvailableRefs(listOf(oldest, "HEAD"))
        assertEquals(1, refs.count { it.value == oldest })
        assertEquals(1, refs.count { it.value == HEAD_REF })
    }

    @Test
    fun unsafePinnedRefIsDropped() {
        assertFalse(git.getAvailableRefs(listOf("--output=x")).any { it.value == "--output=x" })
    }

    @Test
    fun labelWithoutRefNameOrSubject() {
        assertEquals("abc123", GitCli.buildLabel(null, "abc123", ""))
        assertEquals("abc123 │ subject", GitCli.buildLabel("", "abc123", "subject"))
    }

    @Test
    fun headFingerprintChangesOnCommitAndBranchSwitchOnly() {
        val first = git.getHeadFingerprint()!!
        assertEquals(first, git.getHeadFingerprint())

        sh("branch", "other") // a new ref that is not checked out does not change it
        assertEquals(first, git.getHeadFingerprint())

        commit("second")
        val afterCommit = git.getHeadFingerprint()!!
        assertNotEquals(first, afterCommit)

        sh("checkout", "other")
        assertNotEquals(afterCommit, git.getHeadFingerprint())
    }

    @Test
    fun headFingerprintIsNullOutsideARepository() {
        val plain = createPlainDirectory()
        try {
            assertNull(GitCli(plain.path).getHeadFingerprint())
        } finally {
            plain.deleteRecursively()
        }
    }
}
