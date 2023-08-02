package com.cycode.plugin.settings

import com.cycode.plugin.components.settingsWindow.SettingsWindow
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ApplicationSettingsConfigurable(val project: Project) : SearchableConfigurable {
    override fun createComponent(): JComponent {
        return SettingsWindow().getComponent()
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
