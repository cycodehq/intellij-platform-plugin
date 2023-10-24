package com.cycode.plugin

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfo

class Consts {
    companion object {
        val PLUGIN_PATH = PathManager.getPluginsPath() + "/cycode-intellij-platform-plugin"
        val DEFAULT_CLI_PATH = "$PLUGIN_PATH/cycode" + if (SystemInfo.isWindows) ".exe" else ""
        const val CLI_GITHUB_ORG = "cycodehq-public"
        const val CLI_GITHUB_REPO = "cycode-cli"
        const val CLI_CHECK_NEW_VERSION_EVERY_SEC = 24 * 60 * 60 // 24 hours
    }
}
