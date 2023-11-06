package com.cycode.plugin.activities

import com.cycode.plugin.services.CycodeService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PostStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val service = project.service<CycodeService>()
        service.installCliIfNeededAndCheckAuthentication()

        thisLogger().info("PostStartupActivity finished.")
    }
}
