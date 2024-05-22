package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.actions

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

open class CardActions {
    private val gbc = GridBagConstraints()
    private val panel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

    init {
        gbc.insets = JBUI.insets(2)

        panel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1, 0, 0, 0),
            JBUI.Borders.empty(10, 0)
        )
    }

    fun addActionButton(text: String, onClick: () -> Unit) {
        panel.add(JButton(text).apply {
            addActionListener { onClick() }
        }, gbc)
    }

    fun getContent(): JComponent {
        return panel
    }
}
