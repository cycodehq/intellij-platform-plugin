package com.cycode.plugin.components.toolWindow.components.loadingContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.createWrappedHtmlPane
import com.cycode.plugin.services.CycodeService
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class LoadingContentTab : Component<CycodeService>() {
    override fun getContent(service: CycodeService): JPanel {
        // JBLoadingPanel's loading text is a single line label and does not wrap, so build the page by hand
        val content = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(12)
            add(JBLabel(AnimatedIcon.Default()), BorderLayout.NORTH)
            add(createWrappedHtmlPane(CycodeBundle.message("loadingTabLoadingText")), BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(content).apply {
                border = JBUI.Borders.empty()
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }, BorderLayout.CENTER)
        }
    }
}
