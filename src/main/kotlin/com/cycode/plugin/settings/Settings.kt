package com.cycode.plugin.settings

data class Settings (
    val cliAutoManaged: Boolean,
    val cliPath: String,
    val cliApiUrl: String,
    val cliAppUrl: String,
    val cliAdditionalParams: String,
    val scanOnSave: Boolean
)
