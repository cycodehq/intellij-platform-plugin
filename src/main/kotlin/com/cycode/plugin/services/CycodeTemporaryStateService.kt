package com.cycode.plugin.services

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.StatusResult
import com.cycode.plugin.git.HEAD_REF
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.RefOption
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

    // per-repository and therefore session-only; persisting them globally leaks picks between projects
    // scan types that are currently running; drives the spinner in the scan tab
    val runningScans: MutableSet<CliScanType> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    // collapsed state of the scan options section in the tool window (session-only)
    @Volatile
    var scanControlsCollapsed: Boolean = false

    @Volatile
    var diffFromRef: String = LOCAL_DIRECTORY_REF

    @Volatile
    var diffToRef: String = HEAD_REF

    @Volatile
    var diffRefs: List<RefOption> = emptyList()

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
