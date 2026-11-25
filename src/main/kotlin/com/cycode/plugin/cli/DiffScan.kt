package com.cycode.plugin.cli

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
