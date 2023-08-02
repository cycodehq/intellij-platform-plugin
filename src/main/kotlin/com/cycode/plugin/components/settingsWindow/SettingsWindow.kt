package com.cycode.plugin.components.settingsWindow

import com.cycode.plugin.services.pluginState
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class SettingsWindow {
    fun getComponent(): JComponent {
        val pluginState = pluginState()

        val contentPanel = panel {
            row {
                cell {
                    checkBox(
                        "Enable CLI auto management",
                        getter = { pluginState.cliAutoManaged },
                        setter = { pluginState.cliAutoManaged = it },
                    )
                }
            }
            row {
                cell {
                    label("Path to CLI:")
                    textField(
                        getter = { pluginState.cliPath },
                        setter = { pluginState.cliPath = it },
                    )
                }
            }
        }

        return contentPanel
    }
}
