package com.cycode.plugin

import com.cycode.plugin.utils.getPluginVersion
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfo

private fun getDefaultCliPath(): String {
    if (SystemInfo.isWindows) {
        return "${Consts.PLUGIN_PATH}/cycode.exe"
    }

    if (SystemInfo.isMac) {
        // on macOS, we are always using onedir mode because of gatekeeper
        return "${Consts.PLUGIN_PATH}/cycode-cli/cycode-cli"
    }

    return "${Consts.PLUGIN_PATH}/cycode"
}

private fun getSentryReleaseVersion(): String {
    val appName = CycodeBundle.message("appName")
    val version = getPluginVersion()
    return "$appName@${version}"
}

class Consts {
    companion object {
        val PLUGIN_PATH = PathManager.getPluginsPath() + "/cycode-intellij-platform-plugin"
        val DEFAULT_CLI_PATH = getDefaultCliPath()
        const val REQUIRED_CLI_VERSION = "1.10.7"

        const val CYCODE_DOMAIN = "cycode.com"

        const val CLI_GITHUB_ORG = "cycodehq"
        const val CLI_GITHUB_REPO = "cycode-cli"
        const val CLI_GITHUB_TAG = "v$REQUIRED_CLI_VERSION"
        const val CLI_CHECK_NEW_VERSION_EVERY_SEC = 24 * 60 * 60 // 24 hours

        const val PLUGIN_AUTO_SAVE_FLUSH_INITIAL_DELAY_SEC = 0L
        const val PLUGIN_AUTO_SAVE_FLUSH_DELAY_SEC = 5L

        const val SENTRY_DSN = "https://0f0524e8d03a4283702a10ed4b6e03d0@o1026942.ingest.us.sentry.io/4507543885774848"
        const val SENTRY_DEBUG = false
        val SENTRY_RELEASE = getSentryReleaseVersion()
        const val SENTRY_SAMPLE_RATE = 1.0
        const val SENTRY_SEND_DEFAULT_PII = false
        const val SENTRY_ENABLE_UNCAUGHT_EXCEPTION_HANDLER = false
    }
}
