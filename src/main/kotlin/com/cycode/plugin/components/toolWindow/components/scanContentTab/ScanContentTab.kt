package com.cycode.plugin.components.toolWindow.components.scanContentTab

import com.cycode.plugin.Consts
import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.BorderedPanel
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.services.CycodeService
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel

class ScanContentTab : Component<CycodeService>() {
    override fun getContent(service: CycodeService): JPanel {
        val panel = JPanel().apply {
            layout = GridBagLayout()
            add(add(JPanel().apply {
                add(createClickableLabel(CycodeBundle.message("scanTabTitleLabel")))
            }), GridBagConstraints().apply {
                gridy = 0
                insets = JBUI.insetsBottom(10)
                anchor = GridBagConstraints.NORTHWEST
            })
            add(add(JPanel().apply {
                add(createClickableLabel(CycodeBundle.message("scanTabPreButtonsLabel")))
            }), GridBagConstraints().apply {
                gridy = 1
                insets = JBUI.insetsBottom(10)
                anchor = GridBagConstraints.NORTHWEST
            })
            add(JButton(CycodeBundle.message("scanTabSecretsBtn")).apply {
                addActionListener {
                    service.startSecretScanForCurrentFile()
                }
            }, GridBagConstraints().apply {
                gridy = 2
                insets = JBUI.insetsBottom(10)
                fill = GridBagConstraints.HORIZONTAL
            })
            add(add(JPanel().apply {
                add(createClickableLabel(CycodeBundle.message("scanTabOnSaveTip")))
            }), GridBagConstraints().apply {
                gridy = 4
                insets = JBUI.insetsBottom(10)
                anchor = GridBagConstraints.NORTHWEST
            })
            add(add(JPanel().apply {
                add(createClickableLabel(CycodeBundle.message("howToUseLabel")))
            }), GridBagConstraints().apply {
                gridy = 5
                insets = JBUI.insetsBottom(10)
                anchor = GridBagConstraints.NORTHWEST
            })
        }

        if (Consts.EXPERIMENTAL_SCA_SUPPORT) {
            panel.apply {
                add(JButton(CycodeBundle.message("scanTabScaBtn")).apply {
                    addActionListener {
                        service.startScaScanForCurrentProject()
                    }
                }, GridBagConstraints().apply {
                    gridy = 3
                    insets = JBUI.insetsBottom(10)
                    fill = GridBagConstraints.HORIZONTAL
                })
            }
        }

        return BorderedPanel().apply {
            add(panel, BorderLayout.NORTH)
        }
    }
}
