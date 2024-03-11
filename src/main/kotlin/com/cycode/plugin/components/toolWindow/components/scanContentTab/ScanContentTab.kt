package com.cycode.plugin.components.toolWindow.components.scanContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.services.CycodeService
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class ScanContentTab : Component<CycodeService>() {
    private val panel = JPanel(GridBagLayout())
    private val gbc = GridBagConstraints().apply {
        gridy = 0
        insets = JBUI.insetsBottom(10)
        anchor = GridBagConstraints.NORTHWEST
        fill = GridBagConstraints.HORIZONTAL
    }

    private fun addComponentToPanel(component: JComponent) {
        gbc.gridy++
        panel.add(component, gbc)
    }

    override fun getContent(service: CycodeService): JPanel {
        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanTabTitleLabel")))
        addComponentToPanel(
            JButton(CycodeBundle.message("scanTabSecretsBtn")).apply {
                addActionListener { service.startSecretScanForCurrentProject() }
            },
        )
        addComponentToPanel(
            JButton(CycodeBundle.message("scanTabScaBtn")).apply {
                addActionListener { service.startScaScanForCurrentProject() }
            },
        )
        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanTabOnSaveTip")))
        addComponentToPanel(createClickableLabel(CycodeBundle.message("howToUseLabel")))

        return panel
    }
}
