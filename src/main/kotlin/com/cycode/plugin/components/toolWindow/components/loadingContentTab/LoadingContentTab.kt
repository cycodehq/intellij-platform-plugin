package com.cycode.plugin.components.toolWindow.components.loadingContentTab

import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.BorderedPanel
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.services.CycodeService
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

class LoadingContentTab : Component<CycodeService>() {
    override fun getContent(service: CycodeService): JPanel {
        return BorderedPanel().apply {
            add(JPanel().apply {
                layout = GridBagLayout()
                add(add(JPanel().apply {
                    add(createClickableLabel("Cycode is loading..."))
                }), GridBagConstraints().apply {
                    gridy = 0
                    insets = JBUI.insetsBottom(10)
                    anchor = GridBagConstraints.NORTHWEST
                })
            }, BorderLayout.NORTH)
        }
    }
}
