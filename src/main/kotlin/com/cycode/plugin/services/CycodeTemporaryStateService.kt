package com.cycode.plugin.services

import com.cycode.plugin.cli.models.StatusResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.PROJECT)
class CycodeTemporaryStateService {
    var cliInstalled: Boolean = false
    var cliAuthed: Boolean = false

    var cliStatus: StatusResult? = null
        set(value) {
            field = value
            thisLogger().info("cliStatus set")
        }

    val isSecretScanningEnabled: Boolean
        get() = cliStatus?.supportedModules?.secretScanning == true

    val isScaScanningEnabled: Boolean
        get() = cliStatus?.supportedModules?.scaScanning == true

    val isIacScanningEnabled: Boolean
        get() = cliStatus?.supportedModules?.iacScanning == true

    val isSastScanningEnabled: Boolean
        get() = cliStatus?.supportedModules?.sastScanning == true

    val isAiLargeLanguageModelEnabled: Boolean
        get() = cliStatus?.supportedModules?.aiLargeLanguageModel == true

    init {
        thisLogger().info("CycodeTemporaryStateService init")
    }
}
