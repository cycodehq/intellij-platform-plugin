package com.cycode.plugin.services

import com.cycode.plugin.cli.models.StatusResult
import com.cycode.plugin.git.DiffRefsCache
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

    // the "git is not available" message is shown once per session
    @Volatile
    var gitUnavailableNotified: Boolean = false

    // per-repository and therefore session-only; persisting them globally leaks picks between projects
    @Volatile
    var diffFromRef: String = LOCAL_DIRECTORY_REF

    @Volatile
    var diffToRef: String = HEAD_REF

    // the revision lists and the git state they were built for (see GitRepository.getHeadFingerprint)
    @Volatile
    var diffRefsCache: DiffRefsCache? = null

    val diffRefs: List<RefOption>
        get() = diffRefsCache?.refs ?: emptyList()

    // null until known; only an explicit "not a git repository" answer makes it false
    @Volatile
    var isGitRepository: Boolean? = null

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
