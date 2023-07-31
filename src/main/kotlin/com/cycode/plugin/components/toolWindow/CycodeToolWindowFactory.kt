package com.cycode.plugin.components.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.components.authContentTab.AuthContentTab
import com.cycode.plugin.components.toolWindow.components.scanContentTab.ScanContentTab
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.CycodeService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class CycodeToolWindowFactory : ToolWindowFactory {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.title = CycodeBundle.message("name")
        toolWindow.setIcon(PluginIcons.TOOL_WINDOW)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.service<CycodeService>()

        val authTabContent = ContentFactory.SERVICE.getInstance()
            .createContent(AuthContentTab().getContent(service), CycodeBundle.message("toolWindowAuthTab"), false)

        val scanTabContent = ContentFactory.SERVICE.getInstance()
            .createContent(ScanContentTab().getContent(service), CycodeBundle.message("toolWindowScanTab"), false)

        toolWindow.contentManager.addContent(authTabContent)
        toolWindow.contentManager.addContent(scanTabContent)
    }

    override fun shouldBeAvailable(project: Project) = true
}
