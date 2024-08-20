package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.summary

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

open class CardSummary {
    fun getContent(title: String, text: String): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.weightx = 1.0

        val panel = JPanel(GridBagLayout())

        gbc.gridy = 1
        gbc.insets = JBUI.insetsBottom(5)
        panel.add(JBLabel(title).apply {
            font = font.deriveFont(18f).deriveFont(JBFont.BOLD)
        }, gbc)

        gbc.gridy++
        gbc.insets = JBUI.emptyInsets()

        panel.add(JBLabel(text).apply { isAllowAutoWrapping = true; setCopyable(true) }, gbc)

        return panel
    }
}