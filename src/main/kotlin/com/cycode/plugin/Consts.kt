package com.cycode.plugin

import com.intellij.openapi.application.PathManager

class Consts {
    companion object {
        val PLUGIN_PATH = PathManager.getPluginsPath() + "/cycode-intellij-platform-plugin"
        val CLI_PATH = "$PLUGIN_PATH/cycode"
    }
}
