package com.cycode.plugin.cli.models.scanResult.secret

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.DetectionBase

const val IDE_ENTRY_LINE_NUMBER = 1

data class SecretDetection(
    override val id: String,
    override val severity: String,
    override val detectionDetails: SecretDetectionDetails,
    val message: String,
    val type: String,
    val detectionRuleId: String,  // UUID
    val detectionTypeId: String,  // UUID
) : DetectionBase {
    override fun getFormattedMessage(): String {
        return message.replace("within '' repository", "")  // BE bug
    }

    fun getFormattedTitle(): String {
        return CycodeBundle.message("secretsTitle", type, getFormattedMessage())
    }

    override fun getFormattedNodeTitle(): String {
        return CycodeBundle.message("secretsNodeTitle", detectionDetails.line + IDE_ENTRY_LINE_NUMBER, type)
    }
}
