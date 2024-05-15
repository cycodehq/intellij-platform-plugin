package com.cycode.plugin.cli.models.scanResult.sast

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase

data class SastDetection(
    val message: String,
    override val detectionDetails: SastDetectionDetails,
    override val severity: String,
    val type: String,
    val detectionRuleId: String,  // UUID
    val detectionTypeId: String,  // UUID
) : DetectionBase {
    override fun getFormattedMessage(): String {
        return this.detectionDetails.policyDisplayName
    }

    fun getFormattedTitle(): String {
        return CycodeBundle.message("sastTitle", getFormattedMessage())
    }

    override fun getFormattedNodeTitle(): String {
        return CycodeBundle.message("sastNodeTitle", detectionDetails.lineInFile, getFormattedMessage())
    }
}
