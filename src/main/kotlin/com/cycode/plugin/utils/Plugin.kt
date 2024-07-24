package com.cycode.plugin.utils

import com.cycode.plugin.Consts
import com.cycode.plugin.services.pluginSettings
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

fun getPluginVersion(): String {
    return PluginManagerCore.getPlugin(PluginId.getId("com.cycode.plugin"))?.version ?: "unknown"
}

fun isOnPremiseInstallation(): Boolean {
    return !pluginSettings().cliApiUrl.endsWith(Consts.CYCODE_DOMAIN)
}
