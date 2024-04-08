package com.cycode.plugin.components.toolWindow.components.violationCardContentTab

import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

fun convertMarkdownToHtml(markdown: String): String {
    val extensions: List<Extension> = listOf(
        AutolinkExtension.create(), // support for plain URLs in text
        TablesExtension.create(), // support for GitHub Flavored Markdown tables
        StrikethroughExtension.create(), // support for strikethrough text
    )

    val parser = Parser.builder()
        .extensions(extensions)
        .build()
    val renderer = HtmlRenderer.builder()
        .extensions(extensions)
        .build()

    return renderer.render(parser.parse(markdown))
}
