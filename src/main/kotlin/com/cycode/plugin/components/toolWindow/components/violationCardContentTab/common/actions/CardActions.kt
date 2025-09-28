package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.actions

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

open class CardActions {
    private val gbc = GridBagConstraints()
    private val panel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
    private val buttons: MutableMap<String, ButtonInfo> = mutableMapOf()

    private data class ButtonInfo(
        val button: JButton,
        val originalText: String,
        val inProgressText: String
    )

    init {
        gbc.insets = JBUI.insets(2)

        panel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1, 0, 0, 0),
            JBUI.Borders.empty(10, 0)
        )
    }

    fun addActionButton(
        id: String,
        text: String,
        onClick: () -> Unit,
        async: Boolean = false,
        inProgressText: String = "$text..."
    ): JButton {
        val button = JButton(text).apply {
            addActionListener {
                disableButton(id)
                if (async) {
                    // For async operations, onClick handles its own threading and re-enabling
                    onClick()
                } else {
                    // For sync operations, run in a background thread to avoid blocking EDT
                    Thread {
                        try {
                            onClick()
                        } finally {
                            SwingUtilities.invokeLater {
                                enableButton(id)
                            }
                        }
                    }.start()
                }
            }
        }
        buttons[id] = ButtonInfo(button, text, inProgressText)
        panel.add(button, gbc)
        return button
    }

    fun removeButton(id: String) {
        buttons[id]?.let { buttonInfo ->
            panel.remove(buttonInfo.button)
            buttons.remove(id)
            panel.revalidate()
            panel.repaint()
        }
    }

    fun showButton(id: String) {
        buttons[id]?.button?.isVisible = true
        panel.revalidate()
        panel.repaint()
    }

    fun hideButton(id: String) {
        buttons[id]?.button?.isVisible = false
        panel.revalidate()
        panel.repaint()
    }

    fun getButton(id: String): JButton? {
        return buttons[id]?.button
    }

    fun enableButton(id: String) {
        buttons[id]?.let { buttonInfo ->
            SwingUtilities.invokeLater {
                buttonInfo.button.isEnabled = true
                buttonInfo.button.text = buttonInfo.originalText
            }
        }
    }

    fun disableButton(id: String) {
        buttons[id]?.let { buttonInfo ->
            SwingUtilities.invokeLater {
                buttonInfo.button.isEnabled = false
                buttonInfo.button.text = buttonInfo.inProgressText
            }
        }
    }

    fun getContent(): JComponent {
        return panel
    }
}
