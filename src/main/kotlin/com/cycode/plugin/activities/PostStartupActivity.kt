package com.cycode.plugin.activities

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.managers.CliManager
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.services.pluginState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PostStartupActivity : StartupActivity.DumbAware {
    private val cliManager = CliManager()

    private val pluginState = pluginState()
    private val pluginSettings = pluginSettings()

    override fun runActivity(project: Project) {
        // TODO(MarshalX): change to OG org; move to config.
        val owner = "ilya-siamionau-org"
        val repo = "cycode-cli"

        object : Task.Backgroundable(project, CycodeBundle.message("pluginLoading"), false) {
            override fun run(indicator: ProgressIndicator) {
                if (pluginSettings.cliAutoManaged && cliManager.maybeDownloadCli(owner, repo, pluginSettings.cliPath)) {
                    println("CLI was successfully downloaded/updated")
                }

                if (cliManager.healthCheck()) {
                    pluginState.cliInstalled = true
                } else {
                    println("CLI is not working. Need to handle somehow")
                }
            }
        }.queue()

        println("PostStartupActivity finished.")
    }
}
