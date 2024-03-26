package com.cycode.plugin.cli.models.scanResult.iac

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase

data class IacDetection(
    val message: String,
    override val detectionDetails: IacDetectionDetails,
    override val severity: String,
    val type: String,
    val detectionRuleId: String,  // UUID
    val detectionTypeId: String,  // UUID
) : DetectionBase {
    fun getFormattedMessage(): String {
        return message
    }

    fun getFormattedTitle(): String {
        return CycodeBundle.message("iacTitle", type, getFormattedMessage())
    }

    override fun getFormattedNodeTitle(): String {
        return CycodeBundle.message("iacNodeTitle", detectionDetails.lineInFile, message)
    }
}
