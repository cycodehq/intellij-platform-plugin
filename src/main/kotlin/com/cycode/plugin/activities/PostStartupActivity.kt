package com.cycode.plugin.activities

import com.cycode.plugin.annotators.CycodeAnnotator
import com.cycode.plugin.sentry.SentryInit
import com.cycode.plugin.services.cycode
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PostStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        SentryInit.init()

        // we are using singleton here because runActivity is called for each project
        CycodeAnnotator.INSTANCE.registerForAllLangs()

        // synchronized method inside
        cycode(project).installCliIfNeededAndCheckAuthentication()

        thisLogger().info("PostStartupActivity finished.")
    }
}
