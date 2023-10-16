package com.cycode.plugin.cli

import com.cycode.plugin.services.pluginSettings
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.BaseOutputReader
import java.nio.charset.Charset


class CliOSProcessHandler(commandLine: GeneralCommandLine) : OSProcessHandler(commandLine) {
    override fun readerOptions(): BaseOutputReader.Options {
        return BaseOutputReader.Options.forMostlySilentProcess()
    }
}


class CliWrapper(val executablePath: String) {
    val pluginSettings = pluginSettings()

    var mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val defaultCliArgs = arrayOf("-o", "json", "--user-agent", getUserAgent())

    inline fun <reified T> executeCommand(vararg arguments: String): CliResult<T> {
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

        commandLine.addParameters(*defaultCliArgs)

        val additionalArgs = pluginSettings.cliAdditionalParams.split(" ").filterNot { it.isBlank() }.toTypedArray()
        if (additionalArgs.isNotEmpty()) {
            commandLine.addParameters(*additionalArgs)
        }

        commandLine.addParameters(*arguments)

        thisLogger().warn("CLI command: $commandLine")

        val processHandler = CliOSProcessHandler(commandLine)
        val outputListener = OutputListener()
        processHandler.addProcessListener(outputListener)
        processHandler.startNotify()

        processHandler.waitFor()

        val exitCode = processHandler.exitCode
        val stdout = outputListener.stdout.trim().toString()
        val stderr = outputListener.stderr.trim().toString()

        thisLogger().warn("CLI exitCode: $exitCode; stdout: $stdout; stderr: $stderr")

        return try {
            val result: T = mapper.readValue(stdout)
            CliResult.Success(result)
        } catch (e: Exception) {
            // TODO(MarshalX): handle parse errors objects. For example from scan
            thisLogger().error("Failed to parse CLI output: $stdout", e)
            CliResult.Error(exitCode, stderr)
        }
    }

    class OutputListener : ProcessListener {
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
