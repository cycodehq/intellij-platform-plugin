package com.cycode.plugin.services

import com.cycode.plugin.Consts
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
    var cliPath: String = Consts.DEFAULT_CLI_PATH

    var cliApiUrl: String = "https://api.cycode.com"
    var cliAppUrl: String = "https://app.cycode.com"
    var cliAdditionalParams: String = ""

    var scanOnSave: Boolean = true

    var scaSyncFlow: Boolean = true
    var sastSupport: Boolean = false

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
            scaSyncFlow,
            sastSupport,
        )
    }
}
