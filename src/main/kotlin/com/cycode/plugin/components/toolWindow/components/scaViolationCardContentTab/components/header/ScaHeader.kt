package com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.header

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ScaHeader {
    private val gbc = GridBagConstraints()
    private val panel: JPanel = JPanel(GridBagLayout())

    private fun addHeader(label: String, value: String) {
        gbc.gridy++

        panel.add(JLabel(label), gbc.apply {
            gridx = 0
            weightx = 0.125
        })
        panel.add(JBLabel(value).apply { setAllowAutoWrapping(true); setCopyable(true) }, gbc.apply {
            gridx = 1
            weightx = 0.875
            anchor = GridBagConstraints.NORTHWEST
        })
    }

    fun getContent(detection: ScaDetection): JComponent {
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.insets = JBUI.insets(2)

        addHeader(CycodeBundle.message("scaViolationCardHeaderPackageField"), detection.detectionDetails.packageName)
        addHeader(CycodeBundle.message("scaViolationCardHeaderVersionField"), detection.detectionDetails.packageVersion)

        if (detection.detectionDetails.alert != null) {
            val patchedVersion = detection.detectionDetails.alert.firstPatchedVersion
                ?: CycodeBundle.message("scaViolationCardHeaderPatchedVersionDefaultValue")
            addHeader(CycodeBundle.message("scaViolationCardHeaderPatchedVersionField"), patchedVersion)
        }

        if (detection.detectionDetails.dependencyPaths.isNotEmpty()) {
            addHeader(
                CycodeBundle.message("scaViolationCardHeaderDependencyPathField"),
                detection.detectionDetails.dependencyPaths
            )
        }

        if (detection.detectionDetails.alert == null) {
            // if non-permissive-license
            addHeader(
                CycodeBundle.message("scaViolationCardHeaderLicenseField"),
                detection.detectionDetails.license ?: CycodeBundle.message("scaViolationCardHeaderLicenseDefaultValue")
            )
        }

        panel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1, 0, 1, 0),
            JBUI.Borders.empty(10, 0)
        )

        return panel
    }
}
