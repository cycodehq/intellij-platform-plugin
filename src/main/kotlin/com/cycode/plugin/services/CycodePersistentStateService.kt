package com.cycode.plugin.services

import com.cycode.plugin.Consts
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import java.nio.file.Files
import java.nio.file.Paths

@Service
@State(
    name = "CycodePersistentStateService",
    storages = [Storage("cycode.state.xml", roamingType = RoamingType.DISABLED)] // settings sharing is disabled
    // roaming must be disabled in light services:
    // https://plugins.jetbrains.com/docs/intellij/plugin-services.html#light-service-restrictions
)
class CycodePersistentStateService : PersistentStateComponent<CycodePersistentStateService> {
    var cliVer: String? = null
    var cliHash: String? = null
    var cliDirHashes: Map<String, String>? = null
    var cliLastUpdateCheckedAt: Long? = null

    override fun getState(): CycodePersistentStateService {
        return this
    }

    override fun loadState(state: CycodePersistentStateService) {
        // create plugin directory.
        // this is where we'll store the CLI.
        Files.createDirectories(Paths.get(Consts.PLUGIN_PATH))

        XmlSerializerUtil.copyBean(state, this)
    }
}
