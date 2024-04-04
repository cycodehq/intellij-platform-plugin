package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common

import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

open class CommonViolationCardContentTab {
    fun getContent(components: List<JComponent>): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.gridx = 0
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.insetsBottom(10)

        val panel = JPanel(GridBagLayout())

        components.dropLast(1).forEach { component ->
            gbc.gridy++
            panel.add(component, gbc)
        }

        gbc.gridy++
        panel.add(components.last(), gbc.apply {
            weighty = 1.0 // Expand the summary panel to fill the remaining space
        })

        // FIXME(MarshalX): something gives left border already. that's why left != right here
        panel.border = JBUI.Borders.empty(10, 2, 0, 10)

        return panel
    }
}
