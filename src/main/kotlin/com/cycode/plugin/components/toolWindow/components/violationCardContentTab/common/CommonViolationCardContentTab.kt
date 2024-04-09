package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane

open class CommonViolationCardContentTab {
    fun getContent(components: List<JComponent>): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.gridx = 0
        gbc.weightx = 1.0
        gbc.insets = JBUI.insetsBottom(10)

        val panel = JPanel(GridBagLayout())

        components.forEach { component ->
            gbc.gridy++
            panel.add(component, gbc)
        }

        // blank JLabel to fill the remaining space otherwise the components are centered
        gbc.weighty = 1.0
        panel.add(JLabel(" "), gbc)

        // FIXME(MarshalX): something gives left border already. that's why left != right here
        panel.border = JBUI.Borders.empty(10, 2, 0, 10)

        return JBScrollPane(
            panel,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        ).apply { border = null }
    }
}
