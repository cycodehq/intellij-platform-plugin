package com.cycode.plugin.cli.models.scanResult.sast

import com.cycode.plugin.cli.models.scanResult.ScanDetectionDetailsBase

data class SastDetectionDetails(
    val externalScannerId: String,
    val lineInFile: Int,
    val startPosition: Int,
    val endPosition: Int,
    val fileName: String,
    val filePath: String,
    val cwe: List<String>,
    val owasp: List<String>,
    val category: String,
    val languages: List<String>,
    val description: String,
    val policyDisplayName: String,
    val remediationGuidelines: String?,
    val customRemediationGuidelines: String?,
) : ScanDetectionDetailsBase {
    override fun getFilepath(): String {
        return if (filePath.startsWith("/")) filePath else "/$filePath"
    }
}
