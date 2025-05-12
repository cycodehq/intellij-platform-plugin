package com.cycode.plugin.cli.models.scanResult.iac

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase

data class IacDetection(
    override val id: String,
    override val severity: String,
    override val detectionDetails: IacDetectionDetails,
    val message: String,
    val type: String,
    val detectionRuleId: String,  // UUID
    val detectionTypeId: String,  // UUID
) : DetectionBase {
    override fun getFormattedMessage(): String {
        return message
    }

    fun getFormattedTitle(): String {
        return CycodeBundle.message("iacTitle", getFormattedMessage())
    }

    override fun getFormattedNodeTitle(): String {
        return CycodeBundle.message("iacNodeTitle", detectionDetails.getLineNumber(), getFormattedMessage())
    }
}
