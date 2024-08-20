package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.shortSummary

import com.intellij.ui.components.JBLabel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

open class CardShortSummary {
    fun getContent(shortSummary: String? = null): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.weightx = 1.0

        val panel = JPanel(GridBagLayout())

        if (shortSummary != null) {
            panel.add(JBLabel(shortSummary).apply { isAllowAutoWrapping = true; setCopyable(true) }, gbc)
        }

        return panel
    }
}
