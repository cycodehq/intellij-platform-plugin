package com.cycode.plugin

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


class Consts {
    companion object {
        val PLUGIN_PATH = PathManager.getPluginsPath() + "/cycode-intellij-platform-plugin"
        val DEFAULT_CLI_PATH = getDefaultCliPath()
        const val REQUIRED_CLI_VERSION = "1.9.1"

        const val CLI_GITHUB_ORG = "cycodehq"
        const val CLI_GITHUB_REPO = "cycode-cli"
        const val CLI_GITHUB_TAG = "v$REQUIRED_CLI_VERSION"
        const val CLI_CHECK_NEW_VERSION_EVERY_SEC = 24 * 60 * 60 // 24 hours

        const val PLUGIN_AUTO_SAVE_FLUSH_INITIAL_DELAY_SEC = 0L
        const val PLUGIN_AUTO_SAVE_FLUSH_DELAY_SEC = 5L
    }
}
