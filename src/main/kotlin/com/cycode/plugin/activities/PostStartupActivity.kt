package com.cycode.plugin.activities

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PostStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        // TODO download and install cli
    }
}
