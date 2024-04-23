package com.cycode.plugin.cli.models.scanResult.sast

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase

const val SAST_MAX_TITLE_LENGTH = 100

data class SastDetection(
    val message: String,
    override val detectionDetails: SastDetectionDetails,
    override val severity: String,
    val type: String,
    val detectionRuleId: String,  // UUID
    val detectionTypeId: String,  // UUID
) : DetectionBase {
    fun getFormattedMessage(): String {
        if (message.length <= SAST_MAX_TITLE_LENGTH) {
            return message
        }

        return message.take(SAST_MAX_TITLE_LENGTH).plus("...")
    }

    fun getFormattedTitle(): String {
        return CycodeBundle.message("sastTitle", getFormattedMessage())
    }

    override fun getFormattedNodeTitle(): String {
        return CycodeBundle.message("sastNodeTitle", detectionDetails.lineInFile, getFormattedMessage())
    }
}
