package com.cycode.plugin.cli.models.scanResult.iac

import com.cycode.plugin.cli.models.scanResult.ScanDetectionDetailsBase

data class IacDetectionDetails(
    val info: String,
    val failureType: String,
    val infraProvider: String,
    val lineInFile: Int,
    val startPosition: Int,
    val endPosition: Int,
    val filePath: String,
    val fileName: String,
    val description: String?,
    val remediationGuidelines: String?,
    val customRemediationGuidelines: String?,
) : ScanDetectionDetailsBase {
    override fun getFilepath(): String {
        return fileName
    }
}
