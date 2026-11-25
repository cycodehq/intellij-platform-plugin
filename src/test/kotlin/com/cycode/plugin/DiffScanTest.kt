package com.cycode.plugin

import com.cycode.plugin.cli.*
import com.cycode.plugin.git.DiffSide
import org.junit.Assert.*
import org.junit.Test

class DiffScanTest {
    @Test
    fun commitRangeArgs() {
        assertArrayEquals(
            arrayOf("commit-history", "--commit-range", "a..b", "/repo"),
            getDiffScanArgs(DiffScanMode.CommitRange, "a..b", "/repo")
        )
    }

    @Test
    fun localDiffArgs() {
        assertArrayEquals(
            arrayOf("pre-commit", "--include-unstaged", "--base-ref", "main", "--path", "/repo"),
            getDiffScanArgs(DiffScanMode.LocalDiff, "main", "/repo")
        )
    }

    @Test
    fun optionLikeRefIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            getDiffScanArgs(DiffScanMode.LocalDiff, "--output=x", "/repo")
        }
        assertFalse(isSafeRefArg("a\nb"))
        assertTrue(isSafeRefArg("feature/x"))
    }

    @Test
    fun iacDoesNotSupportDiff() {
        assertFalse(supportsDiffScan(CliScanType.Iac))
        assertTrue(supportsDiffScan(CliScanType.Secret))
    }

    @Test
    fun scanTargetArgs() {
        assertArrayEquals(arrayOf("path", "/a", "/b"), ScanTarget.Paths(listOf("/a", "/b")).toCliArgs())
        assertArrayEquals(
            arrayOf("commit-history", "--commit-range", "x..y", "/repo"),
            ScanTarget.Diff(DiffScanMode.CommitRange, "x..y", "/repo").toCliArgs()
        )
    }

    @Test
    fun unsafeRefsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { assertSafeRefArg("") }
        assertThrows(IllegalArgumentException::class.java) { assertSafeRefArg("a\u0000b") }
        assertThrows(IllegalArgumentException::class.java) { getDiffScanArgs(DiffScanMode.CommitRange, "-x", "/repo") }
        assertThrows(IllegalArgumentException::class.java) { DiffSide.Rev("--output=x") }
        assertThrows(IllegalArgumentException::class.java) { DiffSide.Rev("") }
        assertFalse(isSafeRefArg(""))
        assertFalse(isSafeRefArg("-"))
        assertTrue(isSafeRefArg("origin/feature-1"))
        assertTrue(isSafeRefArg("HEAD~1"))
    }

    @Test
    fun everyScanTypeExceptIacSupportsDiff() {
        CliScanType.entries.forEach { assertEquals(it != CliScanType.Iac, supportsDiffScan(it)) }
    }
}
