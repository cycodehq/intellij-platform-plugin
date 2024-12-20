package com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.components.actions

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliIgnoreType
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.common.actions.CardActions
import com.cycode.plugin.services.cycode
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SecretActions(val project: Project) : CardActions() {
    fun addContent(detection: SecretDetection): JComponent {
        addActionButton(CycodeBundle.message("violationCardIgnoreViolationBtn"), onClick = {
            if (detection.detectionDetails.detectedValue != null) {
                cycode(project).applyIgnoreFromFileAnnotation(
                    CliScanType.Secret,
                    CliIgnoreType.VALUE,
                    detection.detectionDetails.detectedValue!!
                )
            }
        })

        return getContent()
    }
}
