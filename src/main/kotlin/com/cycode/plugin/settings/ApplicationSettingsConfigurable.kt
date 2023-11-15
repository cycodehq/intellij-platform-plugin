package com.cycode.plugin.settings

import com.cycode.plugin.Consts
import com.cycode.plugin.components.settingsWindow.SettingsWindow
import com.cycode.plugin.services.cycode
import com.cycode.plugin.services.pluginSettings
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import java.io.File
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import javax.swing.JComponent


class ApplicationSettingsConfigurable(val project: Project) : SearchableConfigurable {
    private val cycodeService = cycode(project)
    private val pluginSettings = pluginSettings()
    private val settingsWindows = SettingsWindow()

    override fun createComponent(): JComponent {
        return settingsWindows.getComponent()
    }

    override fun isModified(): Boolean {
        return pluginSettings.getSettings() != settingsWindows.getSettings()
    }

    private fun isValidCliPath(cliPath: String): Boolean {
        try {
            val cliFile = File(cliPath)

            if (!cliFile.isFile) return false
            if (!cliFile.canExecute()) return false
        } catch (e: Exception) {
            return false
        }

        return true
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            // toURI() method is important here as it ensures that any URL string that complies with RFC 2396
            URL(url).toURI()
            true
        } catch (e: MalformedURLException) {
            false
        } catch (e: URISyntaxException) {
            false
        }
    }

    override fun apply() {
        val newSettings = settingsWindows.getSettings()

        pluginSettings.cliAutoManaged = newSettings.cliAutoManaged
        pluginSettings.scanOnSave = newSettings.scanOnSave
        pluginSettings.cliAdditionalParams = newSettings.cliAdditionalParams

        if (isValidCliPath(newSettings.cliPath)) {
            pluginSettings.cliPath = newSettings.cliPath
        } else {
            pluginSettings.cliPath = Consts.DEFAULT_CLI_PATH
        }

        if (isValidUrl(newSettings.cliApiUrl)) {
            pluginSettings.cliApiUrl = newSettings.cliApiUrl
        }

        if (isValidUrl(newSettings.cliAppUrl)) {
            pluginSettings.cliAppUrl = newSettings.cliAppUrl
        }

        // run the same checks as on the plugin start after updating of settings
        cycodeService.installCliIfNeededAndCheckAuthentication()
    }

    override fun getDisplayName(): String {
        return "Cycode"
    }

    override fun getId(): String {
        return "com.cycode.plugin.settings.ApplicationSettingsConfigurable"
    }
}
