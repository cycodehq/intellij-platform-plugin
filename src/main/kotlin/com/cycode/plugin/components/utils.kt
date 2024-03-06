package com.cycode.plugin.components

import com.intellij.ide.BrowserUtil

fun openURL(url: String) {
    BrowserUtil.browse(url)
}
