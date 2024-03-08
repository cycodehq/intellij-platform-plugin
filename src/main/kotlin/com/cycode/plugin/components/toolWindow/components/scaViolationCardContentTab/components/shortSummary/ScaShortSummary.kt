package com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.shortSummary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.intellij.ui.components.JBLabel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ScaShortSummary {
    fun getContent(detection: ScaDetection): JComponent {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.weightx = 1.0

        val panel = JPanel(GridBagLayout())

        if (detection.detectionDetails.alert != null) {
            val cwe = detection.detectionDetails.vulnerabilityId
            val severity = detection.severity
            val shortSummary = CycodeBundle.message("scaViolationCardShortSummary", severity, cwe ?: "")

            panel.add(JBLabel(shortSummary).apply { setAllowAutoWrapping(true); setCopyable(true) }, gbc)
        }

        return panel
    }
}
