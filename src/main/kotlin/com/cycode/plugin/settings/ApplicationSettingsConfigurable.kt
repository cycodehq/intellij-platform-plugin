package com.cycode.plugin.settings

import com.cycode.plugin.components.settingsWindow.SettingsWindow
import com.cycode.plugin.services.pluginSettings
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ApplicationSettingsConfigurable(val project: Project) : SearchableConfigurable {
    private val pluginSettings = pluginSettings()
    private val settingsWindows = SettingsWindow()

    override fun createComponent(): JComponent {
        return settingsWindows.getComponent()
    }

    override fun isModified(): Boolean {
        return pluginSettings.cliAutoManaged != settingsWindows.getCliAutoManaged() ||
                pluginSettings.cliPath != settingsWindows.getCliPath() ||
                pluginSettings.cliApiUrl != settingsWindows.getCliApiUrl() ||
                pluginSettings.cliAppUrl != settingsWindows.getCliAppUrl() ||
                pluginSettings.cliAdditionalParams != settingsWindows.getCliAdditionalParams() ||
                pluginSettings.scanOnSave != settingsWindows.getScanOnSave()
    }

    override fun apply() {
        pluginSettings.cliAutoManaged = settingsWindows.getCliAutoManaged()
        pluginSettings.cliPath = settingsWindows.getCliPath()
        pluginSettings.cliApiUrl = settingsWindows.getCliApiUrl()
        pluginSettings.cliAppUrl = settingsWindows.getCliAppUrl()
        pluginSettings.cliAdditionalParams = settingsWindows.getCliAdditionalParams()
        pluginSettings.scanOnSave = settingsWindows.getScanOnSave()
    }

    override fun getDisplayName(): String {
        return "Cycode"
    }

    override fun getId(): String {
        return "com.cycode.plugin.settings.ApplicationSettingsConfigurable"
    }
}
