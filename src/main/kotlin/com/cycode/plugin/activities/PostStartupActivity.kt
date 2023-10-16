package com.cycode.plugin.activities

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.managers.CliManager
import com.cycode.plugin.services.pluginSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PostStartupActivity : StartupActivity.DumbAware {
    private val cliManager = CliManager()

    private val pluginSettings = pluginSettings()

    override fun runActivity(project: Project) {
        // TODO(MarshalX): change to OG org; move to config.
        val owner = "ilya-siamionau-org"
        val repo = "cycode-cli"

        object : Task.Backgroundable(project, CycodeBundle.message("pluginLoading"), false) {
            override fun run(indicator: ProgressIndicator) {
                if (pluginSettings.cliAutoManaged && cliManager.shouldDownloadCli(pluginSettings.cliPath)) {
                    cliManager.downloadCli(owner, repo, pluginSettings.cliPath)
                    thisLogger().info("CLI was successfully downloaded/updated")
                }

                cliManager.checkAuth()
                updateToolWindowState(project)
            }
        }.queue()

        thisLogger().info("PostStartupActivity finished.")
    }
}
