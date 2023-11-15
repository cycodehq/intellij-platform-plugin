package com.cycode.plugin.activities

import com.cycode.plugin.services.cycode
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PostStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        cycode(project).installCliIfNeededAndCheckAuthentication()

        thisLogger().info("PostStartupActivity finished.")
    }
}
