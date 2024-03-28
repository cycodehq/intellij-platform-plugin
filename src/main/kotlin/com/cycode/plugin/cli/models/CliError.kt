package com.cycode.plugin.cli.models

data class CliError(
    // FIXME(MarshalX): sometimes CLI uses `code` and sometimes `error` for the same thing
    val code: String? = "Unknown",
    val error: String? = "Unknown",
    val message: String,
    val softFail: Boolean? = false,
)
