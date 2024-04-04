package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.title

import com.cycode.plugin.icons.PluginIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

open class CardTitle {
    private fun getSeverityIcon(severity: String): JLabel {
        val icon = when (severity.toLowerCase()) {
            "critical" -> PluginIcons.CARD_SEVERITY_CRITICAL
            "high" -> PluginIcons.CARD_SEVERITY_HIGH
            "medium" -> PluginIcons.CARD_SEVERITY_MEDIUM
            "low" -> PluginIcons.CARD_SEVERITY_LOW
            else -> PluginIcons.CARD_SEVERITY_INFO
        }

        return JLabel(icon)
    }

    fun getContent(severity: String, titleMessage: String): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.gridy = 0

        val panel = JPanel(GridBagLayout())

        val severityIcon = getSeverityIcon(severity)
        val title = JBLabel(titleMessage).apply {
            setAllowAutoWrapping(true)
            setCopyable(true)
            font = font.deriveFont(18f)
        }

        panel.add(severityIcon, gbc.apply {
            gridx = 0
            weightx = 0.0
            anchor = GridBagConstraints.CENTER
            insets = JBUI.insets(5)
        })
        panel.add(title, gbc.apply {
            gridx = 1
            weightx = 1.0
        })

        return panel
    }
}
