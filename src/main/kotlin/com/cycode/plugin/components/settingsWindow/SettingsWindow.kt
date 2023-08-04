package com.cycode.plugin.components.settingsWindow

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.services.pluginSettings
import com.cycode.plugin.settings.Settings
import com.intellij.ui.layout.panel
import javax.swing.JCheckBox
import javax.swing.JTextField
import javax.swing.JComponent

class SettingsWindow {
    private val pluginSettings = pluginSettings()

    private var cliAutoManagedCheckbox =
        JCheckBox(CycodeBundle.message("settingsCliAutoManagedCheckbox"), pluginSettings.cliAutoManaged)
    private var cliPathTextField = JTextField(pluginSettings.cliPath)

    private var cliApiUrlTextField = JTextField(pluginSettings.cliApiUrl)
    private var cliAppUrlTextField = JTextField(pluginSettings.cliAppUrl)
    private var cliAdditionalParamsTextField = JTextField(pluginSettings.cliAdditionalParams)

    private var scanOnSaveCheckbox =
        JCheckBox(CycodeBundle.message("settingsScanOnSaveCheckbox"), pluginSettings.scanOnSave)

    fun getComponent(): JComponent {
        val contentPanel = panel {
            row {
                cell {
                    cliAutoManagedCheckbox()
                }
            }
            row {
                cell {
                    label(CycodeBundle.message("settingsCliPathLabel"))
                    cliPathTextField()
                }
            }
            row {
                cell {
                    label(CycodeBundle.message("settingsCliApiUrlLabel"))
                    cliApiUrlTextField()
                }
            }
            row {
                cell {
                    label(CycodeBundle.message("settingsCliAppUrlLabel"))
                    cliAppUrlTextField()
                }
            }
            row {
                cell {
                    label(CycodeBundle.message("settingsCliAdditionalParamsLabel"))
                    cliAdditionalParamsTextField()
                }
            }
            row {
                cell {
                    scanOnSaveCheckbox()
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
}
