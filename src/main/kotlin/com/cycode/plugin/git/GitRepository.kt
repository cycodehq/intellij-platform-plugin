package com.cycode.plugin.git

import com.cycode.plugin.cli.isSafeRefArg
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.thisLogger
import java.io.File
import java.nio.charset.StandardCharsets

const val LOCAL_DIRECTORY_REF = "\u0000local"
const val HEAD_REF = "HEAD"
const val LABEL_SEPARATOR = " │ "

/** [value] is what gets passed to git/CLI; [label] is what the user sees. */
data class RefOption(val value: String, val label: String) {
    override fun toString() = label
}

sealed class CommitRangeResult {
    data class Range(val range: String) : CommitRangeResult()
    object SameCommit : CommitRangeResult()
    object NoCommonHistory : CommitRangeResult()
    object UnknownRevision : CommitRangeResult()
    object NotARepository : CommitRangeResult()
    object GitFailure : CommitRangeResult()
}

data class GitOutput(val exitCode: Int, val stdout: String, val stderr: String)

/**
 * Thin wrapper over the git executable. Always run on a background thread.
 * Git is invoked with an argv list (never a shell string); exit codes keep their git semantics
 * because the user-facing messages depend on them.
 */
class GitRepository(private val workDirectory: String) {
    fun run(vararg args: String): GitOutput? {
        return try {
            val commandLine = GeneralCommandLine("git")
                .withParameters(*args)
                .withWorkDirectory(File(workDirectory))
                .withCharset(StandardCharsets.UTF_8)
            val output = CapturingProcessHandler(commandLine).runProcess(GIT_TIMEOUT_MS)
            if (output.isTimeout || output.isCancelled) {
                thisLogger().warn("git ${args.joinToString(" ")} timed out or was cancelled")
                return null
            }
            GitOutput(output.exitCode, output.stdout.trim(), output.stderr.trim())
        } catch (e: Exception) {
            // git is missing or could not be started
            thisLogger().warn("Failed to run git ${args.joinToString(" ")}", e)
            null
        }
    }

    fun isGitAvailable(): Boolean {
        val output = run("--version")
        return output != null && output.exitCode == 0
    }

    /** Only an explicit "not a git repository" answer returns false; other failures are left to later steps. */
    fun isGitRepository(): Boolean {
        val output = run("rev-parse", "--is-inside-work-tree") ?: return true
        if (output.exitCode == 0) {
            return output.stdout == "true"
        }

        return !output.stderr.contains("not a git repository", ignoreCase = true)
    }

    private fun resolveCommit(rev: String): Pair<String?, CommitRangeResult?> {
        val output = run("rev-parse", "--verify", "--quiet", "$rev^{commit}")
        return when {
            output == null -> null to CommitRangeResult.GitFailure
            output.exitCode == 0 -> output.stdout to null
            output.exitCode == 1 -> null to CommitRangeResult.UnknownRevision
            output.stderr.contains("not a git repository", ignoreCase = true) ->
                null to CommitRangeResult.NotARepository

            else -> null to CommitRangeResult.GitFailure
        }
    }

    /**
     * Resolves a "what did [to] add relative to [from]" range as `<merge-base>..<tip>` of full SHAs.
     * Plain `from..to` is empty when picked newest-first, and `from...to` would also scan commits
     * that landed on the other branch since divergence.
     */
    fun getCommitRange(from: String, to: String): CommitRangeResult {
        if (!isSafeRefArg(from) || !isSafeRefArg(to)) {
            return CommitRangeResult.UnknownRevision
        }

        val (fromSha, fromError) = resolveCommit(from)
        if (fromSha == null) return fromError!!
        val (toSha, toError) = resolveCommit(to)
        if (toSha == null) return toError!!

        if (fromSha == toSha) {
            return CommitRangeResult.SameCommit
        }

        val mergeBase = run("merge-base", fromSha, toSha)
            ?: return CommitRangeResult.GitFailure
        if (mergeBase.exitCode == 1) {
            return CommitRangeResult.NoCommonHistory
        }
        if (mergeBase.exitCode != 0 || mergeBase.stdout.isEmpty()) {
            return CommitRangeResult.GitFailure
        }

        val base = mergeBase.stdout
        // "to" is an ancestor of "from": the user picked newest-first
        return if (base == toSha) {
            CommitRangeResult.Range("$toSha..$fromSha")
        } else {
            CommitRangeResult.Range("$base..$toSha")
        }
    }

    /**
     * HEAD first, then branches/tags, then recent commits, plus any [pinnedRefs] (the user's selection)
     * that scrolled out of the lists but still resolve. The caps are deliberate for now.
     */
    fun getAvailableRefs(pinnedRefs: List<String> = emptyList()): List<RefOption> {
        val options = mutableListOf<RefOption>()
        val listedValues = mutableSetOf<String>()
        val listedShas = mutableSetOf<String>()

        fun add(option: RefOption, sha: String? = null) {
            if (listedValues.add(option.value)) {
                options.add(option)
                sha?.let { listedShas.add(it) }
            }
        }

        add(RefOption(HEAD_REF, HEAD_REF))

        val branchOutput = run("rev-parse", "--abbrev-ref", "HEAD")
        val currentBranch = branchOutput?.takeIf { it.exitCode == 0 && it.stdout != HEAD_REF }?.stdout

        val refs = run(
            "for-each-ref", "--sort=-committerdate", "--count=$MAX_REFS",
            "--format=%(refname:short)%09%(objectname)%09%(objectname:short)%09%(contents:subject)",
            "refs/heads", "refs/tags", "refs/remotes"
        )
        if (refs != null && refs.exitCode == 0) {
            for (line in refs.stdout.lines().filter { it.isNotBlank() }) {
                val parts = line.split("\t", limit = 4)
                if (parts.size < 3 || parts[0].endsWith("/HEAD") || !isSafeRefArg(parts[0])) continue
                add(RefOption(parts[0], buildLabel(parts[0], parts[2], parts.getOrElse(3) { "" })), parts[1])
            }
        }

        val log = run("log", "-n", "$MAX_COMMITS", "--format=%H%x09%h%x09%s")
        if (log != null && log.exitCode == 0) {
            for (line in log.stdout.lines().filter { it.isNotBlank() }) {
                val parts = line.split("\t", limit = 3)
                // a commit that is also a branch/tag tip is listed once, under the ref name
                if (parts.size < 2 || parts[0] in listedShas) continue
                // every commit of `git log` is an ancestor of HEAD, so labelling it with the current branch is accurate
                add(RefOption(parts[0], buildLabel(currentBranch, parts[1], parts.getOrElse(2) { "" })))
            }
        }

        for (pinned in pinnedRefs) {
            if (pinned == LOCAL_DIRECTORY_REF || pinned in listedValues || !isSafeRefArg(pinned)) continue
            val (sha, _) = resolveCommit(pinned)
            if (sha == null) continue
            val subject = run("log", "-n", "1", "--format=%h%x09%s", sha)?.stdout?.split("\t", limit = 2)
            add(RefOption(pinned, buildLabel(pinned, subject?.getOrNull(0) ?: sha.take(7), subject?.getOrNull(1) ?: "")))
        }

        return options
    }

    companion object {
        const val MAX_REFS = 20
        const val MAX_COMMITS = 20
        private const val GIT_TIMEOUT_MS = 30_000

        fun buildLabel(refName: String?, shortHash: String, subject: String): String {
            return listOfNotNull(refName?.takeIf { it.isNotEmpty() }, shortHash, subject.takeIf { it.isNotEmpty() })
                .joinToString(LABEL_SEPARATOR)
        }
    }
}
