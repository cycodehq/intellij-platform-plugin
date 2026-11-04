package com.cycode.plugin

import com.cycode.plugin.components.toolWindow.components.treeView.withoutRunningScanTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeExpansionTest {
    private val secrets = listOf("Hardcoded Secrets")
    private val secretsFile = listOf("Hardcoded Secrets", "src/a.kt")
    private val sca = listOf("Open Source Threat")
    private val scaFile = listOf("Open Source Threat", "package.json")
    private val all = setOf(secrets, secretsFile, sca, scaFile)

    @Test
    fun nothingRunningKeepsEverythingExpanded() {
        assertEquals(all, withoutRunningScanTypes(all) { false })
    }

    @Test
    fun onlyTheRunningScanTypeStartsOver() {
        val kept = withoutRunningScanTypes(all) { it == "Hardcoded Secrets" }
        assertEquals(setOf(sca, scaFile), kept)
    }

    @Test
    fun scanAllResetsEveryType() {
        assertTrue(withoutRunningScanTypes(all) { true }.isEmpty())
    }

    @Test
    fun anEmptySetStaysEmpty() {
        assertTrue(withoutRunningScanTypes(emptySet()) { true }.isEmpty())
    }
}
