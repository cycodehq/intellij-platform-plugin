package com.cycode.plugin.cli.models.scanResult.secret

import com.cycode.plugin.cli.models.scanResult.ScanDetectionDetailsBase

data class SecretDetectionDetails(
    val sha512: String,
    val provider: String,
    val concreteProvider: String,
    val length: Int,
    val startPosition: Int,
    val line: Int,
    val committedAt: String,  // TODO(MarshalX): actually DateTime. annotate?
    val filePath: String,
    val fileName: String,
    val fileExtension: String,
) : ScanDetectionDetailsBase {
    override fun getFilepath(): String {
        return "$filePath$fileName"
    }
}
