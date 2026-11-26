package com.cycode.plugin

import com.cycode.plugin.cli.models.scanResult.iac.IacDetectionDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectionPathTest {
    private fun iac(fileName: String) = IacDetectionDetails(
        info = "",
        failureType = "",
        infraProvider = "",
        lineInFile = 1,
        startPosition = 0,
        endPosition = 0,
        filePath = "",
        fileName = fileName,
        description = null,
        remediationGuidelines = null,
        customRemediationGuidelines = null,
        policyDisplayName = null,
    )

    @Test
    fun iacPathWithoutTheLeadingSlashGetsOne() {
        // the async scan flow returns the backend's path as it is
        assertEquals(
            "/Users/me/git/NodeGoat/Dockerfile",
            iac("Users/me/git/NodeGoat/Dockerfile").getFilepath()
        )
    }

    @Test
    fun iacAbsolutePathIsKept() {
        assertEquals(
            "/Users/me/git/NodeGoat/Dockerfile",
            iac("/Users/me/git/NodeGoat/Dockerfile").getFilepath()
        )
    }
}
