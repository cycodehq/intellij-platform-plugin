package com.cycode.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.cycode.plugin.CycodeBundle

@Service(Service.Level.PROJECT)
class CycodeService(project: Project) {

    init {
        thisLogger().info(CycodeBundle.message("projectService", project.name))
    }

    fun startAuth() {
        thisLogger().warn("Start auth...")
    }
}
