package com.cycode.plugin.icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon


object PluginIcons {
    val TOOL_WINDOW: Icon = load("/icons/toolWindowIcon.svg")

    val SCAN_TYPE_IAC: Icon = load("/icons/scan-type/IaC.svg")
    val SCAN_TYPE_SAST: Icon = load("/icons/scan-type/SAST.svg")
    val SCAN_TYPE_SCA: Icon = load("/icons/scan-type/SCA.svg")
    val SCAN_TYPE_SECRETS: Icon = load("/icons/scan-type/Secrets.svg")

    val SEVERITY_CRITICAL: Icon = load("/icons/severity/C.svg")
    val SEVERITY_HIGH: Icon = load("/icons/severity/H.svg")
    val SEVERITY_INFO: Icon = load("/icons/severity/I.svg")
    val SEVERITY_LOW: Icon = load("/icons/severity/L.svg")
    val SEVERITY_MEDIUM: Icon = load("/icons/severity/M.svg")

    val CARD_SEVERITY_CRITICAL: Icon = load("/icons/card-severity/C.svg")
    val CARD_SEVERITY_HIGH: Icon = load("/icons/card-severity/H.svg")
    val CARD_SEVERITY_INFO: Icon = load("/icons/card-severity/I.svg")
    val CARD_SEVERITY_LOW: Icon = load("/icons/card-severity/L.svg")
    val CARD_SEVERITY_MEDIUM: Icon = load("/icons/card-severity/M.svg")

    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, PluginIcons::class.java)
    }

    private fun intellijLoad(path: String): Icon {
        return IconLoader.getIcon(path, AllIcons::class.java)
    }

    fun getSeverityIcon(severity: String): Icon {
        return when (severity.toLowerCase()) {
            "critical" -> SEVERITY_CRITICAL
            "high" -> SEVERITY_HIGH
            "medium" -> SEVERITY_MEDIUM
            "low" -> SEVERITY_LOW
            else -> SEVERITY_INFO
        }
    }
}
