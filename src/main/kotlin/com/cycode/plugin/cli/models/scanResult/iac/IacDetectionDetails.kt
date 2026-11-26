package com.cycode.plugin.cli.models.scanResult.iac

import com.cycode.plugin.cli.models.scanResult.ScanDetectionDetailsBase
import java.io.File

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
    val policyDisplayName: String?,
) : ScanDetectionDetailsBase {
    override fun getFilepath(): String {
        // the async scan flow passes the backend's path through as it is, and it can lack the leading slash
        // ("Users/me/project/Dockerfile"), as SAST results always did (see SastDetectionDetails)
        return if (File(fileName).isAbsolute) fileName else "/$fileName"
    }

    override fun getLineNumber(): Int {
        return lineInFile
    }
}
