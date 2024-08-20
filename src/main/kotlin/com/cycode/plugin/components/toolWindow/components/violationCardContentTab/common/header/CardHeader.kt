package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.header

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

open class CardHeader {
    private val gbc = GridBagConstraints()
    private val panel: JPanel = JPanel(GridBagLayout())

    init {
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.insets = JBUI.insets(2)

        panel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1, 0, 1, 0),
            JBUI.Borders.empty(10, 0)
        )
    }

    fun addHeader(label: String, value: String) {
        gbc.gridy++

        panel.add(JLabel(label), gbc.apply {
            gridx = 0
            weightx = 0.125
        })
        panel.add(JBLabel(value).apply { isAllowAutoWrapping = true; setCopyable(true) }, gbc.apply {
            gridx = 1
            weightx = 0.875
            anchor = GridBagConstraints.NORTHWEST
        })
    }

    fun getContent(): JComponent {
        return panel
    }
}
