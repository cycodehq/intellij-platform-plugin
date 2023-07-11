package com.cycode.plugin.toolWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.icons.PluginIcons
import com.cycode.plugin.services.CycodeService
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel


class CycodeToolWindowFactory : ToolWindowFactory {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.title = CycodeBundle.message("name")
        toolWindow.setIcon(PluginIcons.TOOL_WINDOW)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val cycodeToolWindow = CycodeToolWindow(project)

        val content = ContentFactory.SERVICE.getInstance()
            .createContent(cycodeToolWindow.getContent(), CycodeBundle.message("toolWindowAuthTab"), false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    companion object {
        fun getHyperlinkLabel(textKey: String, urlKey: String): HyperlinkLabel =
            HyperlinkLabel(CycodeBundle.message(textKey)).apply {
                addHyperlinkListener { BrowserUtil.browse(CycodeBundle.message(urlKey)) }
            }
    }

    class CycodeToolWindow(project: Project) {
        private val service = project.service<CycodeService>()

        fun getContent(): JPanel {
            val contentPanel = JPanel().apply {
                layout = BorderLayout()
                add(JPanel().apply {
                    layout = GridBagLayout()
                    border = BorderFactory.createEmptyBorder(10, 20, 10, 20)
                    add(add(JPanel().apply {
                        add(JBLabel(CycodeBundle.message("cliReqInfoLabel")))
                        add(getHyperlinkLabel("cliLinkText", "cliLink"))
                    }), GridBagConstraints().apply {
                        gridy = 0
                        insets = JBUI.insetsBottom(10)
                        anchor = GridBagConstraints.NORTHWEST
                    })
                    add(JButton(CycodeBundle.message("authBtn")).apply {
                        addActionListener {
                            service.startAuth()
                        }
                    }, GridBagConstraints().apply {
                        gridy = 1
                        insets = JBUI.insetsBottom(10)
                        fill = GridBagConstraints.HORIZONTAL
                    })
                    add(add(JPanel().apply {
                        add(JBLabel(CycodeBundle.message("howToUseLabel")))
                        add(getHyperlinkLabel("docsLinkText", "docsLink"))
                    }), GridBagConstraints().apply {
                        gridy = 2
                        anchor = GridBagConstraints.NORTHWEST
                    })
                }, BorderLayout.NORTH)
            }

            return contentPanel
        }
    }
}
