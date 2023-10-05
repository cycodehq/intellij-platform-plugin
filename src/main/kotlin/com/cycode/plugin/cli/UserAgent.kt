package com.cycode.plugin.cli

import com.cycode.plugin.cli.models.IDEUserAgent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

private fun retrieveIDEInfo(): IDEUserAgent {
    val appInfo = ApplicationInfo.getInstance()

    val appName = "jetbrains_plugin"
    val appVersion = PluginManagerCore.getPlugin(PluginId.getId("com.cycode.plugin"))?.version ?: "unknown"
    val envName = appInfo.versionName
    val envVersion = appInfo.fullVersion

    return IDEUserAgent(appName, appVersion, envName, envVersion)
}

fun getUserAgent(): String {
    /*Returns a JSON string representing the IDE user agent.

    Example:
      {"app_name":"jetbrains_plugin","app_version":"0.0.1","env_name":"IntelliJ IDEA","env_version":"2021.1"}

     */
    return ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .writeValueAsString(retrieveIDEInfo())
}
