package com.cycode.plugin.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.application.ApplicationInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.*
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.Key
import com.cycode.plugin.services.pluginSettings
import java.nio.charset.Charset


data class IDEUserAgent(
    val app_name: String,
    val app_version: String,
    val env_name: String,
    val env_version: String
)

fun retrieveIDEInfo(): IDEUserAgent {
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
    val ideInfo = retrieveIDEInfo()
    return Gson().toJson(ideInfo)
}


class CliWrapper(private val executablePath: String) {
    private val pluginSettings = pluginSettings()

    private val gson: Gson = Gson()
    private val defaultArgs = arrayOf("-o", "json", "--user-agent", getUserAgent())

    fun executeCommand(vararg arguments: String): CliResult<Map<String, Any>> {
        val commandLine = GeneralCommandLine()
        commandLine.charset = Charset.forName("UTF-8")

//         TODO(MarshalX): set working dir to project root?
//        commandLine.workDirectory = "..."

        commandLine.environment["CYCODE_API_URL"] = pluginSettings.cliApiUrl
        commandLine.environment["CYCODE_APP_URL"] = pluginSettings.cliAppUrl

        if (SystemInfo.isWindows) {
            commandLine.addParameter("/c")
        } else {
            commandLine.exePath = executablePath
        }

        commandLine.addParameters(*defaultArgs)

        val additionalArgs = pluginSettings.cliAdditionalParams.split(" ").filterNot { it.isBlank() }.toTypedArray()
        if (additionalArgs.isNotEmpty()) {
            commandLine.addParameters(*additionalArgs)
        }

        commandLine.addParameters(*arguments)

        thisLogger().warn("CLI command: $commandLine")

        val processHandler = OSProcessHandler(commandLine)
        val outputListener = OutputListener()
        processHandler.addProcessListener(outputListener)
        processHandler.startNotify()

        processHandler.waitFor()

        val exitCode = processHandler.exitCode
        val stdout = outputListener.stdout.trim().toString()
        val stderr = outputListener.stderr.trim().toString()

        thisLogger().warn("CLI exitCode: $commandLine; stdout: $stdout; stderr: $stderr")

        return if (exitCode == 0) {
            try {
                val result = gson.fromJson<Map<String, Any>>(stdout, object : TypeToken<Map<String, Any>>() {}.type)
                CliResult.Success(result)
            } catch (ex: Exception) {
                CliResult.Error(exitCode, "Failed to parse JSON response: ${ex.message}")
            }
        } else {
            CliResult.Error(exitCode, stderr)
        }
    }

    private class OutputListener : ProcessListener {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            if (outputType == ProcessOutputTypes.STDOUT) {
                stdout.append(event.text)
            } else if (outputType == ProcessOutputTypes.STDERR) {
                stderr.append(event.text)
            }
        }

        override fun processTerminated(event: ProcessEvent) {}
        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}
        override fun startNotified(event: ProcessEvent) {}
    }

}

sealed class CliResult<out T> {
    data class Success<T>(val result: T) : CliResult<T>()
    data class Error(val exitCode: Int?, val errorMessage: String) : CliResult<Nothing>()
}
