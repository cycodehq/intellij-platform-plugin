package com.cycode.plugin.cli

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.IDEUserAgent
import com.cycode.plugin.utils.getPluginVersion
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.intellij.openapi.application.ApplicationInfo

private fun retrieveIDEInfo(): IDEUserAgent {
    val appInfo = ApplicationInfo.getInstance()

    val appName = CycodeBundle.message("appName")
    val appVersion = getPluginVersion()
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
