package com.cycode.plugin.cli.models.scanResult.sca

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase

data class ScaDetection(
    val message: String,
    override val detectionDetails: ScaDetectionDetails,
    override val severity: String,
    val type: String,
    val detectionRuleId: String,
    val detectionTypeId: String,
) : DetectionBase {
    override fun getFormattedMessage(): String {
        return message
    }

    fun getFormattedTitle(): String {
        return CycodeBundle.message(
            "scaTitle",
            detectionDetails.packageName,
            detectionDetails.packageVersion,
            // using the message as fallback for non-premise license detections
            detectionDetails.vulnerabilityDescription ?: getFormattedMessage()
        )
    }

    override fun getFormattedNodeTitle(): String {
        return CycodeBundle.message(
            "scaNodeTitle",
            detectionDetails.lineInFile,
            getFormattedTitle(),
        )
    }
}
