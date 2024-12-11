package com.cycode.plugin.cli.models

data class SupportedModulesStatus(
    // TODO(MarshalX): respect enabled/disabled scanning modules
    val secretScanning: Boolean,
    val scaScanning: Boolean,
    val iacScanning: Boolean,
    val sastScanning: Boolean,
    val aiLargeLanguageModel: Boolean,
)

data class StatusResult(
    val program: String,
    val version: String,
    val isAuthenticated: Boolean,
    val userId: String?,
    val tenantId: String?,
    val supportedModules: SupportedModulesStatus,
)
