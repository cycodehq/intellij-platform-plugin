package com.cycode.plugin

import com.cycode.plugin.git.GitCli
import org.junit.After
import org.junit.Before
import java.io.File
import java.nio.file.Files

/**
 * Real temporary git repository with one commit ("base") on `main`: the git semantics are the point of the tests,
 * so nothing is mocked.
 */
abstract class GitTestBase {
    protected lateinit var dir: File
    protected lateinit var git: GitCli

    protected fun sh(vararg args: String): String {
        // signing and the user's global config must not influence the tests
        val command = listOf(
            "git", "-c", "user.name=t", "-c", "user.email=t@t", "-c", "commit.gpgsign=false",
            "-c", "tag.gpgsign=false", *args
        )
        val process = ProcessBuilder(command).directory(dir).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0) { "git ${args.toList()} failed: $out" }
        return out
    }

    protected fun commit(name: String) {
        File(dir, name).writeText(name)
        sh("add", name)
        sh("commit", "-m", name)
    }

    protected fun createPlainDirectory(): File = Files.createTempDirectory("cycode-plain").toFile()

    @Before
    fun createRepository() {
        dir = Files.createTempDirectory("cycode-git").toFile()
        sh("init")
        sh("symbolic-ref", "HEAD", "refs/heads/main") // `init -b` needs git 2.28
        commit("base")
        git = GitCli(dir.path)
    }

    @After
    fun deleteRepository() {
        dir.deleteRecursively()
    }
}
