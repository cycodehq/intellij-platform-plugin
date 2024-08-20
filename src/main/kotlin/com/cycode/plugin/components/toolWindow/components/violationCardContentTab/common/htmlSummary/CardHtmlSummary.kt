package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.htmlSummary

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.text.DefaultCaret

open class CardHtmlSummary {
    fun getContent(panelTitle: String, htmlSummary: String? = null): JComponent {
        val editorPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            background = UIUtil.TRANSPARENT_COLOR
        }

        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        editorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

        (editorPane.caret as DefaultCaret).updatePolicy = DefaultCaret.NEVER_UPDATE

        editorPane.editorKit = HTMLEditorKitBuilder.simple()
        editorPane.text = htmlSummary

        // reset scroll position to top
        editorPane.caretPosition = 0

        editorPane.border = null

        val panel = JPanel(GridBagLayout()).apply {
            add(
                editorPane,
                GridBagConstraints().apply {
                    fill = GridBagConstraints.BOTH
                    anchor = GridBagConstraints.NORTHWEST
                    weightx = 1.0
                    weighty = 1.0
                    gridy = 1
                }
            )
        }

        if (!htmlSummary.isNullOrBlank()) {
            // we don't want to show the label if there is no description
            // editor pane still required to acquire the space on the card panel

            panel.add(
                JBLabel(panelTitle).apply {
                    font = font.deriveFont(18f).deriveFont(JBFont.BOLD)
                },
                GridBagConstraints().apply {
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.NORTHWEST
                    weightx = 1.0
                    weighty = 0.0
                    gridy = 0
                    insets = JBUI.insetsBottom(5)
                }
            )
        }

        return panel
    }
}
