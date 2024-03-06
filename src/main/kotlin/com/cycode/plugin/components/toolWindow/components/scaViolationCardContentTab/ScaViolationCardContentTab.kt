package com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab

import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.header.ScaHeader
import com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.shortSummary.ScaShortSummary
import com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.summary.ScaSummary
import com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.title.ScaTitle
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ScaViolationCardContentTab {
    fun getContent(detection: ScaDetection): JComponent {
        val titlePanel = ScaTitle().getContent(detection)
        val shortSummaryPanel = ScaShortSummary().getContent(detection)
        val headerContentPanel = ScaHeader().getContent(detection)
        val summaryPanel = ScaSummary().getContent(detection)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.gridx = 0
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.insetsBottom(10)

        val panel = JPanel(GridBagLayout())

        gbc.gridy++
        panel.add(titlePanel, gbc)

        gbc.gridy++
        panel.add(shortSummaryPanel, gbc)

        gbc.gridy++
        panel.add(headerContentPanel, gbc)

        gbc.gridy++
        panel.add(summaryPanel, gbc.apply {
            weighty = 1.0 // Expand the summary panel to fill the remaining space
        })

        // FIXME(MarshalX): something gives left border already. that's why left != right here
        panel.border = JBUI.Borders.empty(10, 2, 0, 10)

        return panel
    }
}
