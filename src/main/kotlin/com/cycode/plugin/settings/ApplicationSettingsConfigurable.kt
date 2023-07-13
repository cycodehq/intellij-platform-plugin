package com.cycode.plugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

class ApplicationSettingsConfigurable(val project: Project) : SearchableConfigurable {
    override fun createComponent(): JComponent {
        // TODO: Implement

        // test placeholder
        val contentPanel = JPanel().apply {
            layout = BorderLayout()
            add(JPanel().apply {
                layout = GridBagLayout()
                border = BorderFactory.createEmptyBorder(10, 20, 10, 20)
                add(add(JPanel().apply {
                    add(JBLabel("Cycode settings test"))
                }), GridBagConstraints().apply {
                    gridy = 0
                    insets = JBUI.insetsBottom(10)
                    anchor = GridBagConstraints.NORTHWEST
                })
            }, BorderLayout.NORTH)
        }

        return contentPanel
    }

    override fun isModified(): Boolean {
        // TODO: Implement
        return false
    }

    override fun apply() {
        // TODO: Implement
        return
    }

    override fun getDisplayName(): String {
        return "Cycode"
    }

    override fun getId(): String {
        return "com.cycode.plugin.settings.ApplicationSettingsConfigurable"
    }
}