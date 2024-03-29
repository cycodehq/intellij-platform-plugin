package com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.components.summary

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.scaViolationCardContentTab.convertMarkdownToHtml
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.text.DefaultCaret

class ScaSummary {
    private fun getDescription(detection: ScaDetection): String {
        val descriptionMarkdown = detection.detectionDetails.alert?.description ?: ""
        return convertMarkdownToHtml(descriptionMarkdown)
    }

    fun getContent(detection: ScaDetection): JComponent {
        val description = getDescription(detection)

        val editorPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            background = UIUtil.TRANSPARENT_COLOR
        }

        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        editorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

        (editorPane.caret as DefaultCaret).updatePolicy = DefaultCaret.NEVER_UPDATE

        editorPane.editorKit = UIUtil.getHTMLEditorKit()
        editorPane.text = description

        // reset scroll position to top
        editorPane.caretPosition = 0

        editorPane.border = null

        val panel = JPanel(GridBagLayout()).apply {
            add(
                JScrollPane(
                    editorPane,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                ).apply { border = null },
                GridBagConstraints().apply {
                    fill = GridBagConstraints.BOTH
                    anchor = GridBagConstraints.NORTHWEST
                    weightx = 1.0
                    weighty = 1.0
                    gridy = 1
                }
            )
        }

        if (detection.detectionDetails.alert?.description != null) {
            // we don't want to show the summary label if there is no description
            // editor pane still required to acquire the space on the card panel

            panel.add(
                JBLabel(CycodeBundle.message("scaViolationCardSummaryTitle")).apply {
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
