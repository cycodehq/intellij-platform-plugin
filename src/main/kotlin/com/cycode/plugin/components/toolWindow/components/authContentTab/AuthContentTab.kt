package com.cycode.plugin.components.toolWindow.components.authContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.BorderedPanel
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.services.CycodeService
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel

class AuthContentTab: Component<CycodeService>() {
    override fun getContent(service: CycodeService): JPanel {
        // TODO(MarshalX): rework to jetbrains.ui; think about how to rerender on changed state
        return BorderedPanel().apply {
            add(JPanel().apply {
                layout = GridBagLayout()
                add(add(JPanel().apply {
                    add(createClickableLabel(CycodeBundle.message("cliReqInfoLabel")))
                }), GridBagConstraints().apply {
                    gridy = 0
                    insets = JBUI.insetsBottom(10)
                    anchor = GridBagConstraints.NORTHWEST
                })
                add(JButton(CycodeBundle.message("authBtn")).apply {
                    addActionListener {
                        this.setEnabled(false)
                        service.startAuth()
                    }
                }, GridBagConstraints().apply {
                    gridy = 1
                    insets = JBUI.insetsBottom(10)
                    fill = GridBagConstraints.HORIZONTAL
                })
                add(add(JPanel().apply {
                    add(createClickableLabel(CycodeBundle.message("howToUseLabel")))
                }), GridBagConstraints().apply {
                    gridy = 2
                    anchor = GridBagConstraints.NORTHWEST
                })
            }, BorderLayout.NORTH)
        }
    }
}
