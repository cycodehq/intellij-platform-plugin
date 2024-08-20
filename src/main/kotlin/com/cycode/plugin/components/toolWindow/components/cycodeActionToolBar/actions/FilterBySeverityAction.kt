package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.CycodeContentTab
import com.cycode.plugin.icons.PluginIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import java.util.function.Supplier

private class SeverityFilterManager {
    // FIXME(MarshalX): remove closed projects?

    companion object {
        val INSTANCE = SeverityFilterManager()
    }

    private val severityFilterStates = mutableMapOf<Project, SeverityFilterState>()

    fun getOrCreateState(project: Project): SeverityFilterState {
        return severityFilterStates.getOrPut(project) { SeverityFilterState() }
    }
}


private class SeverityFilterState {
    private val selectedFilters = mutableMapOf<String, Boolean>()

    fun setState(filter: String, selected: Boolean) {
        selectedFilters[filter.lowercase()] = selected
    }

    fun getState(filter: String): Boolean {
        // by default, all filters are selected
        return selectedFilters.getOrDefault(filter.lowercase(), true)
    }

    fun exportState(): Map<String, Boolean> {
        return selectedFilters
    }
}

class FilterBySeverityAction(private val contentTab: CycodeContentTab, private val severity: String) :
    ToggleAction(
        Supplier { CycodeBundle.message("toolbarFilterBySeverityAction", severity) },
        PluginIcons.getSeverityIcon(severity)
    ),
    DumbAware {
    companion object {
        fun create(contentTab: CycodeContentTab, severity: String): FilterBySeverityAction {
            return FilterBySeverityAction(contentTab, severity)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        val stateManager = SeverityFilterManager.INSTANCE.getOrCreateState(project)
        return stateManager.getState(severity)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        val stateManager = SeverityFilterManager.INSTANCE.getOrCreateState(project)
        stateManager.setState(severity, state)

        contentTab.getTreeView().updateSeverityFilter(stateManager.exportState())
    }
}
