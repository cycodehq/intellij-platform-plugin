package com.cycode.plugin.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.execution.process.*
import com.intellij.openapi.util.Key
import java.nio.charset.Charset

class CliWrapper(private val executablePath: String) {
    private val gson: Gson = Gson()

    fun executeCommand(vararg arguments: String): CliResult<Map<String, Any>> {
        val commandLine = GeneralCommandLine()
        commandLine.charset = Charset.forName("UTF-8")
//         TODO set working dir to project root
//        commandLine.workDirectory = "..."
//         TODO: set envs from plugin settings
//        commandLine.environment...
        if (SystemInfo.isWindows) {
            commandLine.addParameter("/c")
        } else {
            commandLine.exePath = executablePath
        }

        commandLine.addParameters(*arguments)

        val processHandler = OSProcessHandler(commandLine)
        val outputListener = OutputListener()
        processHandler.addProcessListener(outputListener)
        processHandler.startNotify()

        processHandler.waitFor()

        val exitCode = processHandler.exitCode
        val stdout = outputListener.stdout.trim().toString()
        val stderr = outputListener.stderr.trim().toString()

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
