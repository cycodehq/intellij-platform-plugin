package com.cycode.plugin

import com.cycode.plugin.components.toolWindow.ToolWindowAction
import com.cycode.plugin.components.toolWindow.ToolWindowUpdate
import com.cycode.plugin.components.toolWindow.resolveToolWindowAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolWindowUpdateTest {
    @Test
    fun layoutChangesAlwaysRebuildAndKeepTheResults() {
        listOf(true, false).forEach { scanTabShown ->
            assertEquals(
                ToolWindowAction.Rebuild(reloadResults = false),
                resolveToolWindowAction(ToolWindowUpdate.LayoutChanged, scanTabShown)
            )
        }
    }

    @Test
    fun runningStateChangesNeverReplaceWhatIsShown() {
        assertEquals(
            ToolWindowAction.RefreshScanTab(reloadResults = false),
            resolveToolWindowAction(ToolWindowUpdate.RunningStateChanged, scanTabShown = true)
        )
        // e.g. a finding is open while an on-save scan finishes
        assertEquals(
            ToolWindowAction.RefreshSpinners,
            resolveToolWindowAction(ToolWindowUpdate.RunningStateChanged, scanTabShown = false)
        )
    }

    @Test
    fun stateChangesRefreshInPlaceWhenPossibleAndAlwaysReloadTheResults() {
        assertEquals(
            ToolWindowAction.RefreshScanTab(reloadResults = true),
            resolveToolWindowAction(ToolWindowUpdate.StateChanged, scanTabShown = true)
        )
        assertEquals(
            ToolWindowAction.Rebuild(reloadResults = true),
            resolveToolWindowAction(ToolWindowUpdate.StateChanged, scanTabShown = false)
        )
    }
}
