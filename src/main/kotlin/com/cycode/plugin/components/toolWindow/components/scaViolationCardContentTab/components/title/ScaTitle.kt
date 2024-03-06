package com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.title

import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.icons.PluginIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ScaTitle {
    private fun getSeverityIcon(detection: ScaDetection): JLabel {
        val icon = when (detection.severity.toLowerCase()) {
            "critical" -> PluginIcons.CARD_SEVERITY_CRITICAL
            "high" -> PluginIcons.CARD_SEVERITY_HIGH
            "medium" -> PluginIcons.CARD_SEVERITY_MEDIUM
            "low" -> PluginIcons.CARD_SEVERITY_LOW
            else -> PluginIcons.CARD_SEVERITY_INFO
        }

        return JLabel(icon)
    }

    fun getContent(detection: ScaDetection): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.gridy = 0

        val panel = JPanel(GridBagLayout())

        val severityIcon = getSeverityIcon(detection)
        val titleMessage = detection.detectionDetails.alert?.summary ?: detection.message
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
