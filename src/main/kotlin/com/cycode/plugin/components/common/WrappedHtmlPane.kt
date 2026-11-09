package com.cycode.plugin.components.common

import com.cycode.plugin.components.openURL
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

/** Read-only HTML text that wraps to the available width and keeps links clickable. */
fun createWrappedHtmlPane(html: String): JEditorPane {
    return SwingHelper.createHtmlViewer(true, null, null, null).apply {
        text = html
        border = JBUI.Borders.empty()
        isOpaque = false
        foreground = UIUtil.getLabelForeground()
        addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                val url = e.url?.toString() ?: e.description
                if (url != null) openURL(url)
            }
        }
    }
}
