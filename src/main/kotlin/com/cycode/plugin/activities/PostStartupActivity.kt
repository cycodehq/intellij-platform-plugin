package com.cycode.plugin.activities

import com.cycode.plugin.Consts
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
    private val pluginSettings = pluginSettings()

    override fun runActivity(project: Project) {
        object : Task.Backgroundable(project, CycodeBundle.message("pluginLoading"), false) {
            val cliManager = CliManager(project)

            override fun run(indicator: ProgressIndicator) {
                // if the CLI path is not overriden and executable is auto managed, and eed to download - download it.
                if (
                    pluginSettings.cliPath == Consts.DEFAULT_CLI_PATH &&
                    pluginSettings.cliAutoManaged &&
                    cliManager.shouldDownloadCli(pluginSettings.cliPath)
                ) {
                    cliManager.downloadCli(pluginSettings.cliPath)
                    thisLogger().info("CLI was successfully downloaded/updated")
                }

                cliManager.checkAuth()
                updateToolWindowState(project)

                // required to know CLI version.
                // unfortunately, we don't have a universal command that will cover the auth state and CLI version yet
                cliManager.healthCheck()
            }
        }.queue()

        thisLogger().info("PostStartupActivity finished.")
    }
}
