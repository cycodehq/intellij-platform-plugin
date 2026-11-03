package com.cycode.plugin.services

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.StatusResult
import com.cycode.plugin.git.HEAD_REF
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.RefOption
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.PROJECT)
class CycodeTemporaryStateService {
    var cliInstalled: Boolean = false
    var cliAuthed: Boolean = false

    var cliStatus: StatusResult? = null
        set(value) {
            field = value
            thisLogger().info("cliStatus set")
        }

    // number of running scans per type (e.g. an on-save scan can overlap a manual one); drives the spinners
    private val runningScanCounts = ConcurrentHashMap<CliScanType, AtomicInteger>()

    fun scanStarted(scanType: CliScanType) {
        runningScanCounts.computeIfAbsent(scanType) { AtomicInteger() }.incrementAndGet()
    }

    fun scanFinished(scanType: CliScanType) {
        runningScanCounts[scanType]?.updateAndGet { maxOf(0, it - 1) }
    }

    fun isScanRunning(scanType: CliScanType): Boolean = (runningScanCounts[scanType]?.get() ?: 0) > 0

    fun hasRunningScans(): Boolean = runningScanCounts.values.any { it.get() > 0 }

    // collapsed state of the scan options section in the tool window (session-only)
    @Volatile
    var scanControlsCollapsed: Boolean = false

    // the "git is not available" message is shown once per session
    @Volatile
    var gitUnavailableNotified: Boolean = false

    // per-repository and therefore session-only; persisting them globally leaks picks between projects
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
