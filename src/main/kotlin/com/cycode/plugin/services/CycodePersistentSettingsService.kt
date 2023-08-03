package com.cycode.plugin.services

import com.cycode.plugin.Consts.Companion.CLI_PATH
import com.cycode.plugin.settings.Settings
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(
    name = "CycodePersistentSettingsService",
    storages = [Storage("cycode.settings.xml", roamingType = RoamingType.DISABLED)] // settings sharing is disabled
)
class CycodePersistentSettingsService : PersistentStateComponent<CycodePersistentSettingsService> {
    var cliAutoManaged: Boolean = true
    var cliPath: String = CLI_PATH

    var cliApiUrl: String = "https://api.cycode.com"
    var cliAppUrl: String = "https://app.cycode.com"
    var cliAdditionalParams: String = ""

    var scanOnSave: Boolean = true

    override fun getState(): CycodePersistentSettingsService {
        return this
    }

    override fun loadState(state: CycodePersistentSettingsService) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getSettings(): Settings {
        return Settings(
            cliAutoManaged,
            cliPath,
            cliApiUrl,
            cliAppUrl,
            cliAdditionalParams,
            scanOnSave,
        )
    }
}
