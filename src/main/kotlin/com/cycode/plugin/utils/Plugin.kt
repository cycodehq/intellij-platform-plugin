package com.cycode.plugin.utils

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

fun getPluginVersion(): String {
    return PluginManagerCore.getPlugin(PluginId.getId("com.cycode.plugin"))?.version ?: "unknown"
}
