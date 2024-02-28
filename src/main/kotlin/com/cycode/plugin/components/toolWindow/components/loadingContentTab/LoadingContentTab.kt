package com.cycode.plugin.components.toolWindow.components.loadingContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.services.CycodeService
import com.intellij.ui.components.JBLoadingPanel
import java.awt.BorderLayout
import javax.swing.JPanel

class LoadingContentTab : Component<CycodeService>() {
    override fun getContent(service: CycodeService): JPanel {
        return JBLoadingPanel(BorderLayout(), service).apply {
            setLoadingText(CycodeBundle.message("loadingTabLoadingText"))
            startLoading()
        }
    }
}
