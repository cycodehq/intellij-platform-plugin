package com.cycode.plugin.cli.models.scanResult.sca

import com.cycode.plugin.cli.models.scanResult.ScanDetectionDetailsBase

data class ScaDetectionDetails(
    val fileName: String,
    val startPosition: Int,
    val endPosition: Int,
    val line: Int,
    val lineInFile: Int,
    val dependencyPaths: String,
    val license: String?,
    val packageName: String,
    val packageVersion: String,
    val vulnerabilityDescription: String?,
    val vulnerabilityId: String?,
    val alert: ScaDetectionDetailsAlert?,
    val description: String?,
    val remediationGuidelines: String?,
    val customRemediationGuidelines: String?,
    val policyDisplayName: String?,
) : ScanDetectionDetailsBase {
    override fun getFilepath(): String {
        return fileName
    }

    override fun getLineNumber(): Int {
        return lineInFile
    }
}
