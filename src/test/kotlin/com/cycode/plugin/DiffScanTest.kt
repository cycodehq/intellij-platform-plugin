package com.cycode.plugin

import com.cycode.plugin.cli.*
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
    fun versionGate() {
        assertTrue(isVersionAtLeast("3.25.0", "3.25.0"))
        assertTrue(isVersionAtLeast("3.26.0", "3.25.0"))
        assertTrue(isVersionAtLeast("4.0.0", "3.25.0"))
        assertFalse(isVersionAtLeast("3.24.9", "3.25.0"))
        assertFalse(isVersionAtLeast("0.0.1.dev498", "3.25.0"))
        assertFalse(isDiffScanSupportedByCli(null))
    }
}
