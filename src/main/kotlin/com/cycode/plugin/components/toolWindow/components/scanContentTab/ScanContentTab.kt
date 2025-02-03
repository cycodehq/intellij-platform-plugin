package com.cycode.plugin.components.toolWindow.components.scanContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.services.CycodeService
import com.cycode.plugin.services.pluginLocalState
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
        // do not get local state on class level because will be outdated
        val pluginLocalState = pluginLocalState(service.project)

        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanTabTitleLabel")))
        addComponentToPanel(
            JButton(CycodeBundle.message("scanTabSecretsBtn")).apply {
                isEnabled = pluginLocalState.isSecretScanningEnabled
                addActionListener { service.startScanForCurrentProject(CliScanType.Secret) }
            },
        )
        addComponentToPanel(
            JButton(CycodeBundle.message("scanTabScaBtn")).apply {
                isEnabled = pluginLocalState.isScaScanningEnabled
                addActionListener { service.startScanForCurrentProject(CliScanType.Sca) }
            },
        )
        addComponentToPanel(
            JButton(CycodeBundle.message("scanTabIacBtn")).apply {
                isEnabled = pluginLocalState.isIacScanningEnabled
                addActionListener { service.startScanForCurrentProject(CliScanType.Iac) }
            },
        )
        addComponentToPanel(
            JButton(CycodeBundle.message("scanTabSastBtn")).apply {
                isEnabled = pluginLocalState.isSastScanningEnabled
                addActionListener { service.startScanForCurrentProject(CliScanType.Sast) }
            },
        )

        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanTabOnSaveTip")))
        addComponentToPanel(createClickableLabel(CycodeBundle.message("howToUseLabel")))

        return panel
    }
}
