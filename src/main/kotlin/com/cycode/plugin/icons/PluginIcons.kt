package com.cycode.plugin.icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon


object PluginIcons {
    val TOOL_WINDOW: Icon = load("/icons/toolWindowIcon.svg")

    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, PluginIcons::class.java)
    }

    private fun intellijLoad(path: String): Icon {
        return IconLoader.getIcon(path, AllIcons::class.java)
    }
}
