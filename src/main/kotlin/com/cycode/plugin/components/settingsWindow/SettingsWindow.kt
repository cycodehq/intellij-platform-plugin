package com.cycode.plugin.components.settingsWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.settings.Settings
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel

class SettingsWindow {
    private val pluginSettings = pluginSettings()

    private var cliAutoManagedCheckbox = JBCheckBox(null, pluginSettings.cliAutoManaged)
    private var cliPathTextField = JBTextField(pluginSettings.cliPath, FIELD_COLUMNS_COUNT)

    private var cliApiUrlTextField = JBTextField(pluginSettings.cliApiUrl, FIELD_COLUMNS_COUNT)
    private var cliAppUrlTextField = JBTextField(pluginSettings.cliAppUrl, FIELD_COLUMNS_COUNT)
    private var cliAdditionalParamsTextField = JBTextField(pluginSettings.cliAdditionalParams, FIELD_COLUMNS_COUNT)

    private var scanOnSaveCheckbox = JBCheckBox(null, pluginSettings.scanOnSave)

    private var scaSyncFlowCheckbox = JBCheckBox(null, pluginSettings.scaSyncFlow)
    private var sastSupportCheckbox = JBCheckBox(null, pluginSettings.sastSupport)

    fun getComponent(): DialogPanel {
        val contentPanel = panel {
            titledRow(CycodeBundle.message("settingsCliSectionTitle")) {
                row(label = CycodeBundle.message("settingsCliAutoManagedCheckbox")) {
                    cell {
                        cliAutoManagedCheckbox()
                    }
                }
                row(label = CycodeBundle.message("settingsCliPathLabel")) {
                    cell {
                        cliPathTextField()
                    }
                }
                row(label = CycodeBundle.message("settingsCliAdditionalParamsLabel")) {
                    cell {
                        cliAdditionalParamsTextField()
                    }
                }
            }
            titledRow(CycodeBundle.message("settingsOnPremiseSectionTitle")) {
                row(label = CycodeBundle.message("settingsCliApiUrlLabel")) {
                    cell {
                        cliApiUrlTextField()
                    }
                }
                row(label = CycodeBundle.message("settingsCliAppUrlLabel")) {
                    cell {
                        cliAppUrlTextField()
                    }
                }
            }
            titledRow(CycodeBundle.message("settingsIdeSectionTitle")) {
                row(label = CycodeBundle.message("settingsScanOnSaveCheckbox")) {
                    cell {
                        scanOnSaveCheckbox()
                    }
                }
            }
            titledRow(CycodeBundle.message("settingsExperimentalSectionTitle")) {
                row(label = CycodeBundle.message("settingsScaSyncFlowCheckbox")) {
                    cell {
                        scaSyncFlowCheckbox()
                    }
                }
                row(label = CycodeBundle.message("settingsSastSupportCheckbox")) {
                    cell {
                        sastSupportCheckbox()
                    }
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
            scaSyncFlowCheckbox.isSelected,
            sastSupportCheckbox.isSelected,
        )
    }

    companion object {
        const val FIELD_COLUMNS_COUNT = 35  // max on the smallest settings window
    }
}
