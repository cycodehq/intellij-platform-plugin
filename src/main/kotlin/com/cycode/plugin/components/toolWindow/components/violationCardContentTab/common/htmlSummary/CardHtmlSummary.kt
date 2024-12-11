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

open class CardHtmlSummary(panelTitle: String) {
    private val editorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        isOpaque = false
        background = UIUtil.TRANSPARENT_COLOR
    }
    private val mainPanel = JPanel(GridBagLayout()).apply {
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
    private val labelComponent = JBLabel(panelTitle).apply {
        font = font.deriveFont(18f).deriveFont(JBFont.BOLD)
    }
    private val labelComponentConstraints = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        anchor = GridBagConstraints.NORTHWEST
        weightx = 1.0
        weighty = 0.0
        gridy = 0
        insets = JBUI.insetsBottom(5)
    }

    init {
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        editorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

        (editorPane.caret as DefaultCaret).updatePolicy = DefaultCaret.NEVER_UPDATE

        editorPane.editorKit = HTMLEditorKitBuilder.simple()
        editorPane.border = null
    }

    fun getContent(htmlSummary: String? = null): JComponent {
        setHtmlContent(htmlSummary)
        return mainPanel
    }

    fun setHtmlContent(htmlSummary: String? = null) {
        editorPane.text = htmlSummary
        // reset scroll position to top
        editorPane.caretPosition = 0

        if (htmlSummary.isNullOrBlank()) {
            // we don't want to show the label if there is no description
            // editor pane still required to acquire the space on the card panel
            mainPanel.remove(labelComponent)
        } else {
            mainPanel.add(labelComponent, labelComponentConstraints)
        }
    }
}
