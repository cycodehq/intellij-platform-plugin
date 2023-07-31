package com.cycode.plugin.components.common

import com.intellij.ide.BrowserUtil
import com.intellij.ui.HyperlinkLabel

fun createClickableLabel(htmlContent: String): HyperlinkLabel {
    val label = HyperlinkLabel()
    label.setHtmlText(htmlContent)

    label.addHyperlinkListener { _ ->
        // FIXME(MarshalX): e.url and e.description are always null.
        //  Temp hack to bypass this. Should not work with many links.
        val url = extractUrlFromHtml(htmlContent)
        if (url != null) {
            openURL(url)
        }
    }

    return label
}

fun extractUrlFromHtml(html: String): String? {
    val regex = """<a\s+[^>]*href="([^"]*)"[^>]*>.*</a>""".toRegex()
    val match = regex.find(html)
    return match?.groupValues?.getOrNull(1)
}

fun openURL(url: String) {
    BrowserUtil.browse(url)
}
