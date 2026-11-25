package com.cycode.plugin.services

import com.cycode.plugin.cli.CliScanType
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

    fun isScanTypeEnabled(type: CliScanType): Boolean = when (type) {
        CliScanType.Secret -> isSecretScanningEnabled
        CliScanType.Sca -> isScaScanningEnabled
        CliScanType.Iac -> isIacScanningEnabled
        CliScanType.Sast -> isSastScanningEnabled
    }

    /** Scan types enabled for the tenant, in the order the scan buttons are shown. */
    val enabledScanTypes: List<CliScanType>
        get() = SCAN_TYPES_IN_ORDER.filter { isScanTypeEnabled(it) }

    val isAiLargeLanguageModelEnabled: Boolean
        get() = cliStatus?.supportedModules?.aiLargeLanguageModel == true

    init {
        thisLogger().info("CycodeTemporaryStateService init")
    }

    companion object {
        val SCAN_TYPES_IN_ORDER = listOf(CliScanType.Secret, CliScanType.Sca, CliScanType.Iac, CliScanType.Sast)
    }
}
