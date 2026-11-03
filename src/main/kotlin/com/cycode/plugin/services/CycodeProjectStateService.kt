package com.cycode.plugin.services

import com.cycode.plugin.cli.ScanScope
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil

/** Settings that belong to one project; stored in the project's workspace file (not shared through VCS). */
@Service(Service.Level.PROJECT)
@State(name = "CycodeProjectStateService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CycodeProjectStateService : PersistentStateComponent<CycodeProjectStateService> {
    var scanScope: String = ScanScope.Project.name

    override fun getState(): CycodeProjectStateService {
        return this
    }

    override fun loadState(state: CycodeProjectStateService) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
