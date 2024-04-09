package com.cycode.plugin.components.toolWindow.components.violationCardContentTab

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension

fun convertMarkdownToHtml(markdown: String): String {
    val options = MutableDataSet()

    val extensions: List<Extension> = listOf(
        AutolinkExtension.create(), // support for plain URLs in text
        TablesExtension.create(), // support for GitHub Flavored Markdown tables
        StrikethroughExtension.create(), // support for strikethrough text
    )
    options.set(Parser.EXTENSIONS, extensions)

    val parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()

    return renderer.render(parser.parse(markdown))
}
