package com.cycode.plugin.cli

import com.cycode.plugin.cli.models.CliError
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
import com.intellij.util.io.BaseOutputReader
import java.io.File
import java.nio.charset.Charset


class CliOSProcessHandler(commandLine: GeneralCommandLine) : OSProcessHandler(commandLine) {
    override fun readerOptions(): BaseOutputReader.Options {
        return BaseOutputReader.Options.forMostlySilentProcess()
    }
}


class CliWrapper(val executablePath: String, val workDirectory: String? = null) {
    val pluginSettings = pluginSettings()

    var mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val defaultCliArgs = arrayOf("-o", "json", "--user-agent", getUserAgent())

    inline fun <reified T> executeCommand(
        vararg arguments: String,
        noinline cancelledCallback: (() -> Boolean)? = null
    ): CliResult<T> {
        val commandLine = GeneralCommandLine()
        commandLine.charset = Charset.forName("UTF-8")
        commandLine.exePath = executablePath

        if (workDirectory != null) {
            commandLine.workDirectory = File(workDirectory)
        }

        commandLine.environment["CYCODE_API_URL"] = pluginSettings.cliApiUrl
        commandLine.environment["CYCODE_APP_URL"] = pluginSettings.cliAppUrl

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

        while (!processHandler.isProcessTerminated) {
            if (cancelledCallback != null && cancelledCallback()) {
                processHandler.destroyProcess()
                return CliResult.Panic(ExitCodes.TERMINATION, "Execution was canceled")
            }

            processHandler.waitFor(WAIT_FOR_DELAY_MS)
        }

        val exitCode = processHandler.exitCode
        val stdout = outputListener.stdout.trim().toString()
        val stderr = outputListener.stderr.trim().toString()

        thisLogger().warn("CLI exitCode: $exitCode; stdout: $stdout; stderr: $stderr")

        if (exitCode == ExitCodes.ABNORMAL_TERMINATION) {
            val errorCode = detectErrorCode(stderr)
            if (errorCode == ErrorCode.UNKNOWN) {
                thisLogger().error("Unknown error with abnormal termination: $stdout; $stderr")
            } else {
                return CliResult.Panic(exitCode, getUserFriendlyCliErrorMessage(errorCode))
            }
        }

        if (T::class == Unit::class) {
            return CliResult.Success(Unit) as CliResult<T>
        }

        try {
            val result: T = mapper.readValue(stdout)
            return CliResult.Success(result)
        } catch (e: Exception) {
            thisLogger().warn("Failed to parse success CLI output: $stdout", e)

            try {
                // FIXME(MarshalX): probably CLI not standardized error format for the whole project yet
                val result: CliError = mapper.readValue(stdout)
                return CliResult.Error(result)
            } catch (e: Exception) {
                thisLogger().error("Failed to parse ANY CLI output: $stdout", e)
            }

            return CliResult.Panic(exitCode, stderr)
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

    companion object {
        const val WAIT_FOR_DELAY_MS = 1000L
    }

}

sealed class CliResult<out T> {
    data class Success<T>(val result: T) : CliResult<T>()
    data class Error(val result: CliError) : CliResult<Nothing>()
    data class Panic(val exitCode: Int?, val errorMessage: String) : CliResult<Nothing>()
}
