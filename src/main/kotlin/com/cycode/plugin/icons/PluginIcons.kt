package com.cycode.plugin.icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon


object PluginIcons {
    val TOOL_WINDOW: Icon = load("/icons/toolWindowIcon.svg")

    val SCAN_TYPE_IAC: Icon = load("/icons/scan-type/IaC.png")
    val SCAN_TYPE_SAST: Icon = load("/icons/scan-type/SAST.png")
    val SCAN_TYPE_SCA: Icon = load("/icons/scan-type/SCA.png")
    val SCAN_TYPE_SECRETS: Icon = load("/icons/scan-type/Secrets.png")

    val SEVERITY_CRITICAL: Icon = load("/icons/severity/C.png")
    val SEVERITY_HIGH: Icon = load("/icons/severity/H.png")
    val SEVERITY_INFO: Icon = load("/icons/severity/I.png")
    val SEVERITY_LOW: Icon = load("/icons/severity/L.png")
    val SEVERITY_MEDIUM: Icon = load("/icons/severity/M.png")

    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, PluginIcons::class.java)
    }

    private fun intellijLoad(path: String): Icon {
        return IconLoader.getIcon(path, AllIcons::class.java)
    }
}
