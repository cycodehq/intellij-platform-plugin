package com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.CycodeContentTab
import com.cycode.plugin.components.toolWindow.components.cycodeActionToolBar.actions.*
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup

class CycodeActionToolbar {
    fun attachActionToolbar(contentTab: CycodeContentTab): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(HomeAction.create(contentTab))
            addSeparator()
            add(RunAllAction.create(contentTab))
            addSeparator()
            add(ExpandAllAction.create(contentTab))
            add(CollapseAllAction.create(contentTab))
            addSeparator()
            add(FilterBySeverityAction.create(contentTab, "Critical"))
            add(FilterBySeverityAction.create(contentTab, "High"))
            add(FilterBySeverityAction.create(contentTab, "Medium"))
            add(FilterBySeverityAction.create(contentTab, "Low"))
            addSeparator()
            add(ClearAction.create(contentTab))
            addSeparator()
            add(SettingsAction.create(contentTab))
            add(HelpAction.create(contentTab))
        }

        val toolbar = ActionManager.getInstance().createActionToolbar(
            CycodeBundle.message("toolbarId"), actionGroup, true
        )
        toolbar.setTargetComponent(contentTab)
        contentTab.toolbar = toolbar.component

        return toolbar
    }
}
