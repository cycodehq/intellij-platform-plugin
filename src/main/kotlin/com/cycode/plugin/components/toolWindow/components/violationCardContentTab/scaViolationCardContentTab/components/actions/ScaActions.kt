package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.components.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliIgnoreType
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.actions.CardActions
import com.cycode.plugin.services.cycode
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ScaActions(val project: Project) : CardActions() {
    fun addContent(detection: ScaDetection): JComponent {
        if (detection.detectionDetails.alert?.cveIdentifier != null) {
            addActionButton(CycodeBundle.message("violationCardIgnoreViolationBtn"), onClick = {
                cycode(project).applyIgnoreFromFileAnnotation(
                    CliScanType.Sca,
                    CliIgnoreType.CVE,
                    detection.detectionDetails.alert.cveIdentifier
                )
            })
        }

        return getContent()
    }
}
