package com.cycode.plugin.cli

import com.cycode.plugin.Consts

enum class ScanScope {
    Project, Diff, File
}

enum class DiffScanMode {
    CommitRange, LocalDiff
}

/** Describes what a single CLI scan invocation should scan. */
sealed class ScanTarget {
    data class Paths(val paths: List<String>) : ScanTarget()
    data class Diff(val mode: DiffScanMode, val ref: String, val path: String) : ScanTarget()
}

fun ScanTarget.toCliArgs(): Array<String> = when (this) {
    is ScanTarget.Paths -> arrayOf("path", *paths.toTypedArray())
    is ScanTarget.Diff -> getDiffScanArgs(mode, ref, path)
}

// IaC is not supported by the CLI's commit-history and pre-commit diff flavours
fun supportsDiffScan(scanType: CliScanType): Boolean = scanType != CliScanType.Iac

fun getDiffScanArgs(mode: DiffScanMode, ref: String, path: String): Array<String> {
    assertSafeRefArg(ref)

    return when (mode) {
        DiffScanMode.CommitRange -> arrayOf("commit-history", "--commit-range", ref, path)
        DiffScanMode.LocalDiff -> arrayOf("pre-commit", "--include-unstaged", "--base-ref", ref, "--path", path)
    }
}

/**
 * Refs are passed to git and the CLI as separate arguments (no shell), but a value like "--output=x"
 * would still be parsed as an option. Reject those and anything containing control characters.
 */
fun assertSafeRefArg(ref: String) {
    require(isSafeRefArg(ref)) { "Unsafe revision: must be non-empty, not start with '-' and have no control characters" }
}

fun isSafeRefArg(ref: String): Boolean = ref.isNotEmpty() && !ref.startsWith("-") && ref.none { it.isISOControl() }

/** Compares only the first three numeric segments; dev builds look like `0.0.1.dev498`. */
fun isVersionAtLeast(version: String, minVersion: String): Boolean {
    fun parse(value: String) = value.trim().removePrefix("v").split(".").take(3)
        .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        .let { it + List(3 - it.size) { 0 } }

    val actual = parse(version)
    val required = parse(minVersion)
    for (i in 0 until 3) {
        if (actual[i] != required[i]) {
            return actual[i] > required[i]
        }
    }

    return true
}

fun isDiffScanSupportedByCli(cliVersion: String?): Boolean {
    return cliVersion != null && isVersionAtLeast(cliVersion, Consts.MIN_CLI_VERSION_FOR_DIFF_SCAN)
}
