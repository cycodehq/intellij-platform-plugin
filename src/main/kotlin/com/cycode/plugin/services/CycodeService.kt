package com.cycode.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.managers.CliManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) {
    private val cliManager = CliManager()

    init {
        thisLogger().info(CycodeBundle.message("projectService", project.name))
    }

    fun startAuth() {
        object : Task.Backgroundable(project, CycodeBundle.message("authProcessing"), true) {
            override fun run(indicator: ProgressIndicator) {
                if (!cliManager.checkAuth()) {
                    // TODO(MarshalX): increase timeout
                    //   process.BaseOSProcessHandler Process hasn't generated any output for a long time.
                    cliManager.doAuth()
                }
            }
        }.queue()
    }
}
