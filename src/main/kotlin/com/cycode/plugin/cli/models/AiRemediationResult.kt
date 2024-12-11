package com.cycode.plugin.cli.models

data class AiRemediationResult(
    val result: Boolean,
    val message: String,
    val data: AiRemediationResultData? = null,
)

data class AiRemediationResultData(
    val remediation: String,
    val isFixAvailable: Boolean,
)
