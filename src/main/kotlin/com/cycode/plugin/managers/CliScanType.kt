package com.cycode.plugin.managers

import com.cycode.plugin.CycodeBundle

enum class CliScanType {
    Secret, Sast, Sca, Iac
}

fun getScanTypeDisplayName(scanType: CliScanType): String {
    return when (scanType) {
        CliScanType.Secret -> CycodeBundle.message("secretDisplayName")
        CliScanType.Sca -> CycodeBundle.message("scaDisplayName")
        CliScanType.Sast -> CycodeBundle.message("sastDisplayName")
        CliScanType.Iac -> CycodeBundle.message("iacDisplayName")
    }
}
