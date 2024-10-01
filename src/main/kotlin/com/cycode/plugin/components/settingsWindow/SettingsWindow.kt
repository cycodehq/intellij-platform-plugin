package com.cycode.plugin.components.settingsWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.settings.Settings
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel

class SettingsWindow {
    private val pluginSettings = pluginSettings()

    private var cliAutoManagedCheckbox = JBCheckBox(null, pluginSettings.cliAutoManaged)
    private var cliPathTextField = JBTextField(pluginSettings.cliPath, FIELD_COLUMNS_COUNT)

    private var cliApiUrlTextField = JBTextField(pluginSettings.cliApiUrl, FIELD_COLUMNS_COUNT)
    private var cliAppUrlTextField = JBTextField(pluginSettings.cliAppUrl, FIELD_COLUMNS_COUNT)
    private var cliAdditionalParamsTextField = JBTextField(pluginSettings.cliAdditionalParams, FIELD_COLUMNS_COUNT)

    private var scanOnSaveCheckbox = JBCheckBox(null, pluginSettings.scanOnSave)

    fun getComponent(): DialogPanel {
        val contentPanel = panel {
            group(CycodeBundle.message("settingsCliSectionTitle")) {
                row(CycodeBundle.message("settingsCliAutoManagedCheckbox")) {
                    cell(cliAutoManagedCheckbox)
                }
                row(CycodeBundle.message("settingsCliPathLabel")) {
                    cell(cliPathTextField)
                }
                row(CycodeBundle.message("settingsCliAdditionalParamsLabel")) {
                    cell(cliAdditionalParamsTextField)
                }
            }
            group(CycodeBundle.message("settingsOnPremiseSectionTitle")) {
                row(CycodeBundle.message("settingsCliApiUrlLabel")) {
                    cell(cliApiUrlTextField)
                }
                row(CycodeBundle.message("settingsCliAppUrlLabel")) {
                    cell(cliAppUrlTextField)
                }
            }
            group(CycodeBundle.message("settingsIdeSectionTitle")) {
                row(CycodeBundle.message("settingsScanOnSaveCheckbox")) {
                    cell(scanOnSaveCheckbox)
                }
            }
        }

        return contentPanel
    }

    fun getSettings(): Settings {
        return Settings(
            cliAutoManagedCheckbox.isSelected,
            cliPathTextField.text,
            cliApiUrlTextField.text,
            cliAppUrlTextField.text,
            cliAdditionalParamsTextField.text,
            scanOnSaveCheckbox.isSelected,
        )
    }

    companion object {
        const val FIELD_COLUMNS_COUNT = 35  // max on the smallest settings window
    }
}
