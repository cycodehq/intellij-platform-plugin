package com.cycode.plugin.activities

import com.cycode.plugin.Consts.Companion.CLI_PATH
import com.cycode.plugin.Consts.Companion.PLUGIN_PATH
import com.cycode.plugin.managers.CliManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.nio.file.Files
import java.nio.file.Paths

class PostStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        // create plugin directory.
        // this is where we'll store the CLI.
        // TODO: move creation to config manager
        Files.createDirectories(Paths.get(PLUGIN_PATH));

        val owner = "MarshalX"
        val repo = "cycode-cli"

        val cliManager = CliManager()
        val downloadedFile = cliManager.downloadLatestRelease(owner, repo, CLI_PATH)

        if (downloadedFile != null) {
            println("Successfully downloaded the latest release to ${downloadedFile.absolutePath}")
        } else {
            println("Failed to download the latest release.")
        }
    }
}
