package com.cycode.plugin

import com.cycode.plugin.components.toolWindow.ScanOptionsCollapse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanOptionsCollapseTest {
    @Test
    fun toggleFlipsTheState() {
        val state = ScanOptionsCollapse()
        assertFalse(state.collapsed)
        state.toggle()
        assertTrue(state.collapsed)
        state.toggle()
        assertFalse(state.collapsed)
    }

    @Test
    fun collapsedSectionExpandsForAFindingAndCollapsesAgainWhenItIsClosed() {
        val state = ScanOptionsCollapse().apply { toggle() }

        assertTrue(state.onCardOpened(cardAlreadyShown = false))
        assertFalse(state.collapsed)

        assertTrue(state.onCardClosed())
        assertTrue(state.collapsed)
    }

    @Test
    fun expandedSectionStaysExpandedAfterAFindingIsClosed() {
        val state = ScanOptionsCollapse()

        assertFalse(state.onCardOpened(cardAlreadyShown = false))
        assertFalse(state.onCardClosed())
        assertFalse(state.collapsed)
    }

    @Test
    fun switchingBetweenFindingsKeepsTheOriginalState() {
        val state = ScanOptionsCollapse().apply { toggle() }

        state.onCardOpened(cardAlreadyShown = false)
        // a second finding replaces the first: the section is already expanded, but must still be restored
        assertFalse(state.onCardOpened(cardAlreadyShown = true))
        assertTrue(state.onCardClosed())
        assertTrue(state.collapsed)
    }

    @Test
    fun leavingAFindingByAnotherRouteDoesNotCollapseTheNextOne() {
        val state = ScanOptionsCollapse().apply { toggle() }
        state.onCardOpened(cardAlreadyShown = false)
        // the user pressed Home instead of the close button and expanded nothing: state is expanded, flag is stale

        // a later finding is opened while the section is expanded
        assertFalse(state.onCardOpened(cardAlreadyShown = false))
        assertFalse(state.onCardClosed())
        assertFalse(state.collapsed)
    }

    @Test
    fun closingWithoutAnOpenFindingDoesNothing() {
        val state = ScanOptionsCollapse()
        assertFalse(state.onCardClosed())
        assertFalse(state.collapsed)
    }
}
