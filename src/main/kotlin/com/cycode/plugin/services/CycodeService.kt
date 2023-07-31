package com.cycode.plugin.services

import com.cycode.plugin.Consts.Companion.CLI_PATH
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.utils.CliWrapper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

@Service(Service.Level.PROJECT)
class CycodeService(val project: Project) {

    init {
        thisLogger().info(CycodeBundle.message("projectService", project.name))
    }

    fun startAuth() {
        object : Task.Backgroundable(project, "Start auth...", true) {
            override fun run(indicator: ProgressIndicator) {
                println(CliWrapper(CLI_PATH).executeCommand("auth", "check"))

                // TODO: increase timeout
                println(CliWrapper(CLI_PATH).executeCommand("auth"))

                println(CliWrapper(CLI_PATH).executeCommand("auth", "check"))
            }
        }.queue()
    }
}
