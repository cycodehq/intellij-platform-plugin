package com.cycode.plugin.components.toolWindow.components.violationCardContentTab

import com.cycode.plugin.cli.models.scanResult.DetectionBase
import com.cycode.plugin.cli.models.scanResult.iac.IacDetection
import com.cycode.plugin.cli.models.scanResult.sast.SastDetection
import com.cycode.plugin.cli.models.scanResult.sca.ScaDetection
import com.cycode.plugin.cli.models.scanResult.secret.SecretDetection
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.iacViolationCardContentTab.IacViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.sastViolationCardContentTab.SastViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.scaViolationCardContentTab.ScaViolationCardContentTab
import com.cycode.plugin.components.toolWindow.components.violationCardContentTab.secretViolationCardContentTab.SecretViolationCardContentTab
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/** The violation card for a finding of any scan type; null for an unknown detection type. */
fun createViolationCard(project: Project, detection: DetectionBase): JComponent? = when (detection) {
    is SecretDetection -> SecretViolationCardContentTab(project).getContent(detection)
    is ScaDetection -> ScaViolationCardContentTab(project).getContent(detection)
    is IacDetection -> IacViolationCardContentTab(project).getContent(detection)
    is SastDetection -> SastViolationCardContentTab(project).getContent(detection)
    else -> null
}
