package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.companyGuidelines

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class SecretSummary {
    fun getContent(detection: SecretDetection): JComponent {
        val message = detection.message.replace(
            "within '' repository", // BE bug
            ""
        )

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.weightx = 1.0

        val panel = JPanel(GridBagLayout())

        gbc.gridy = 1
        gbc.insets = JBUI.insetsBottom(5)
        panel.add(JBLabel(CycodeBundle.message("secretViolationCardSummaryTitle")).apply {
            font = font.deriveFont(18f).deriveFont(JBFont.BOLD)
        }, gbc)

        gbc.gridy++
        gbc.insets = JBUI.emptyInsets()
        panel.add(JBLabel(message).apply { setAllowAutoWrapping(true); setCopyable(true) }, gbc)

        return panel
    }
}
