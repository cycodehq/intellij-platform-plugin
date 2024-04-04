package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.header

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.header.CardHeader
import javax.swing.JComponent

class ScaHeader : CardHeader() {
    fun addContent(detection: ScaDetection): JComponent {
        addHeader(CycodeBundle.message("scaViolationCardHeaderPackageField"), detection.detectionDetails.packageName)
        addHeader(CycodeBundle.message("scaViolationCardHeaderVersionField"), detection.detectionDetails.packageVersion)

        if (detection.detectionDetails.alert != null) {
            val patchedVersion = detection.detectionDetails.alert.firstPatchedVersion
                ?: CycodeBundle.message("scaViolationCardHeaderPatchedVersionDefaultValue")
            addHeader(CycodeBundle.message("scaViolationCardHeaderPatchedVersionField"), patchedVersion)
        }

        if (detection.detectionDetails.dependencyPaths.isNotEmpty()) {
            addHeader(
                CycodeBundle.message("scaViolationCardHeaderDependencyPathField"),
                detection.detectionDetails.dependencyPaths
            )
        }

        if (detection.detectionDetails.alert == null) {
            // if non-permissive-license
            addHeader(
                CycodeBundle.message("scaViolationCardHeaderLicenseField"),
                detection.detectionDetails.license ?: CycodeBundle.message("scaViolationCardHeaderLicenseDefaultValue")
            )
        }

        return getContent()
    }
}
