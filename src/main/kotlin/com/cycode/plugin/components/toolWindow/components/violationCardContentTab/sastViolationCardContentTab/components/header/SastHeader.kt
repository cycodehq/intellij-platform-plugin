package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.components.header

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.header.CardHeader
import javax.swing.JComponent

class SastHeader : CardHeader() {
    fun addContent(detection: SastDetection): JComponent {
        addHeader(CycodeBundle.message("sastViolationCardHeaderFileField"), detection.detectionDetails.fileName)
        addHeader(CycodeBundle.message("sastViolationCardHeaderCategoryField"), detection.detectionDetails.category)

        val langs = detection.detectionDetails.languages.joinToString(", ")
        addHeader(CycodeBundle.message("sastViolationCardHeaderLanguageField"), langs)

        val engineIdToDisplayName = mapOf(
            "5db84696-88dc-11ec-a8a3-0242ac120002" to "Semgrep OSS (Orchestrated by Cycode)",
            "560a0abd-d7da-4e6d-a3f1-0ed74895295c" to "Bearer (Powered by Cycode)"
        )
        val engineDisplayName = engineIdToDisplayName[detection.detectionDetails.externalScannerId]
        if (engineDisplayName != null) {
            addHeader(CycodeBundle.message("sastViolationCardHeaderEngineField"), engineDisplayName)
        }

        addHeader(CycodeBundle.message("sastViolationCardHeaderRuleIdField"), detection.detectionRuleId)

        return getContent()
    }
}
