package com.cycode.plugin.cli.models

data class CliError(
    val code: Int,
    val message: String,
    val softFail: Boolean? = false,
)
