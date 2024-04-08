package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class IacSummary {
    fun getContent(detection: IacDetection): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.weightx = 1.0

        val panel = JPanel(GridBagLayout())

        gbc.gridy = 1
        gbc.insets = JBUI.insetsBottom(5)
        panel.add(JBLabel(CycodeBundle.message("iacViolationCardSummaryTitle")).apply {
            font = font.deriveFont(18f).deriveFont(JBFont.BOLD)
        }, gbc)

        gbc.gridy++
        gbc.insets = JBUI.emptyInsets()
        val message = detection.detectionDetails.description ?: detection.message
        panel.add(JBLabel(message).apply { setAllowAutoWrapping(true); setCopyable(true) }, gbc)

        return panel
    }
}
