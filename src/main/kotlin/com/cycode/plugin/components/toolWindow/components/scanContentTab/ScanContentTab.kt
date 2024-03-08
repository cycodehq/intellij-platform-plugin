package com.cycode.plugin.components.toolWindow.components.scanContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.services.CycodeService
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel

class ScanContentTab : Component<CycodeService>() {
    override fun getContent(service: CycodeService): JPanel {
        val gbc = GridBagConstraints().apply {
            gridy = 0
            insets = JBUI.insetsBottom(10)
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
        }

        val panel = JPanel(GridBagLayout())

        gbc.gridy++
        panel.add(createClickableLabel(CycodeBundle.message("scanTabTitleLabel")), gbc)

        gbc.gridy++
        panel.add(
            JButton(CycodeBundle.message("scanTabSecretsBtn")).apply {
                addActionListener { service.startSecretScanForCurrentProject() }
            },
            gbc
        )

        gbc.gridy++
        panel.add(
            JButton(CycodeBundle.message("scanTabScaBtn")).apply {
                addActionListener { service.startScaScanForCurrentProject() }
            },
            gbc
        )

        gbc.gridy++
        panel.add(createClickableLabel(CycodeBundle.message("scanTabOnSaveTip")), gbc)

        gbc.gridy++
        panel.add(createClickableLabel(CycodeBundle.message("howToUseLabel")), gbc)

        return panel
    }
}
