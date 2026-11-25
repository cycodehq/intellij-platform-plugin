package com.cycode.plugin

import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.isFileSupportedByScanType
import com.cycode.plugin.cli.partitionByFileSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanScopeLogicTest {
    private val allTypes = listOf(CliScanType.Secret, CliScanType.Sca, CliScanType.Sast, CliScanType.Iac)

    @Test
    fun secretAndSastScanAnyFile() {
        assertTrue(isFileSupportedByScanType("/p/Main.kt", CliScanType.Secret))
        assertTrue(isFileSupportedByScanType("/p/Main.kt", CliScanType.Sast))
    }

    @Test
    fun scaOnlyScansPackageFiles() {
        assertTrue(isFileSupportedByScanType("/p/package.json", CliScanType.Sca))
        assertTrue(isFileSupportedByScanType("/p/requirements.txt", CliScanType.Sca))
        assertFalse(isFileSupportedByScanType("/p/Main.kt", CliScanType.Sca))
    }

    @Test
    fun iacOnlyScansInfrastructureFiles() {
        assertTrue(isFileSupportedByScanType("/p/main.tf", CliScanType.Iac))
        assertTrue(isFileSupportedByScanType("/p/Dockerfile", CliScanType.Iac))
        assertTrue(isFileSupportedByScanType("/p/deploy.yaml", CliScanType.Iac))
        assertFalse(isFileSupportedByScanType("/p/Main.kt", CliScanType.Iac))
    }

    @Test
    fun partitionKeepsTheOrderOfTheScanTypes() {
        val (supported, unsupported) = partitionByFileSupport("/p/Main.kt", allTypes)
        assertEquals(listOf(CliScanType.Secret, CliScanType.Sast), supported)
        assertEquals(listOf(CliScanType.Sca, CliScanType.Iac), unsupported)

        val (all, none) = partitionByFileSupport("/p/package.json", listOf(CliScanType.Secret, CliScanType.Sca))
        assertEquals(listOf(CliScanType.Secret, CliScanType.Sca), all)
        assertTrue(none.isEmpty())
    }
}
