package com.cycode.plugin.components.toolWindow

/**
 * Whether the scan options section is collapsed. Opening a finding needs the room, so the section expands for it
 * and is collapsed again when the finding is closed, but only if it was collapsed before.
 */
class ScanOptionsCollapse {
    @Volatile
    var collapsed = false
        private set

    private var collapsedBeforeCard = false

    fun toggle() {
        collapsed = !collapsed
    }

    /** Returns true if the section was collapsed and has to be expanded to make room for the finding. */
    fun onCardOpened(cardAlreadyShown: Boolean): Boolean {
        if (!cardAlreadyShown) {
            collapsedBeforeCard = collapsed
        }
        val mustExpand = collapsed
        collapsed = false
        return mustExpand
    }

    /** Returns true if the section has to be collapsed again because it was collapsed before the finding opened. */
    fun onCardClosed(): Boolean {
        val mustCollapse = collapsedBeforeCard
        collapsedBeforeCard = false
        if (mustCollapse) {
            collapsed = true
        }
        return mustCollapse
    }
}
