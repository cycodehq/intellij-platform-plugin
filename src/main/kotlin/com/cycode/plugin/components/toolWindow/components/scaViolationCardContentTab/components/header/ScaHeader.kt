package com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.header

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

        addHeader("Package: ", detection.detectionDetails.packageName)
        addHeader("Version: ", detection.detectionDetails.packageVersion)

        if (detection.detectionDetails.alert != null) {
            val patchedVersion = detection.detectionDetails.alert.firstPatchedVersion ?: "Not fixed"
            addHeader("First patched version:", patchedVersion)
        }

        if (detection.detectionDetails.dependencyPaths.isNotEmpty()) {
            addHeader("Dependency path:", detection.detectionDetails.dependencyPaths)
        }

        if (detection.detectionDetails.alert == null) {
            // if non-permissive-license
            addHeader("License:", detection.detectionDetails.license ?: "Unknown")
        }

        panel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1, 0, 1, 0),
            JBUI.Borders.empty(10, 0)
        )

        return panel
    }
}
