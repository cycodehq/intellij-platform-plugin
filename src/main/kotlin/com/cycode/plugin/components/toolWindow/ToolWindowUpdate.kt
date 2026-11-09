package com.cycode.plugin.components.toolWindow

/** Why the tool window is being updated; decides how much of it is touched. */
enum class ToolWindowUpdate {
    /** Anything may have changed (results, ignores, auth, CLI status). */
    StateChanged,

    /** A scan started or finished: only buttons and spinners change; whatever is shown is never replaced. */
    RunningStateChanged,

    /** Only the layout changed (scope, collapse, docking): the results are untouched. */
    LayoutChanged,
}

sealed class ToolWindowAction {
    /** Refresh the visible scan tab in place (no flicker) and optionally reload the results tree. */
    data class RefreshScanTab(val reloadResults: Boolean) : ToolWindowAction()

    /** Only swap the spinner icons in the results tree. */
    object RefreshSpinners : ToolWindowAction()

    /** Replace the right panel, optionally reloading the results tree. */
    data class Rebuild(val reloadResults: Boolean) : ToolWindowAction()
}

/** [scanTabShown]: the scan tab is the panel currently on screen and can be refreshed in place. */
fun resolveToolWindowAction(update: ToolWindowUpdate, scanTabShown: Boolean): ToolWindowAction = when (update) {
    ToolWindowUpdate.LayoutChanged -> ToolWindowAction.Rebuild(reloadResults = false)
    ToolWindowUpdate.RunningStateChanged ->
        if (scanTabShown) ToolWindowAction.RefreshScanTab(reloadResults = false) else ToolWindowAction.RefreshSpinners

    ToolWindowUpdate.StateChanged ->
        if (scanTabShown) ToolWindowAction.RefreshScanTab(reloadResults = true) else ToolWindowAction.Rebuild(reloadResults = true)
}
