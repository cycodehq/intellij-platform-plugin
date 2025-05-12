package com.cycode.plugin.annotators.annotationAppliers

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.annotators.convertSeverity
import com.cycode.plugin.annotators.validateTextRange
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.intentions.CycodeOpenViolationCardIntentionQuickFix
import com.cycode.plugin.services.ScanResultsService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class SastApplier(private val scanResults: ScanResultsService) : AnnotationApplierBase() {
    private fun validateSastTextRange(): Boolean {
        // FIXME(MarshalX): for now, I dont see any way to validate the text range for SAST
        //   small explanation:
        //   - SAST doesn't provide end positions, so we have to calculate them from the line number (get the last character in the line)
        //   - we can't use the same validation as for SCA because value in the range is unknown (for SCA we expect package name)
        return true
    }

    override fun apply(psiFile: PsiFile, holder: AnnotationHolder) {
        val latestScanResult = scanResults.sastResults
        if (latestScanResult !is CliResult.Success) {
            return
        }

        val relevantDetections = latestScanResult.result.detections.filter { detection ->
            detection.detectionDetails.getFilepath() == psiFile.virtualFile.path
        }

        relevantDetections.forEach { detection ->
            val severity = convertSeverity(detection.severity)

            // SAST doesn't provide start and end positions, so we have to calculate them from the line number
            val line = detection.detectionDetails.getLineNumber() - 1
            val startOffset = psiFile.text.lines().take(line).sumOf { it.length + 1 }
            val endOffset = startOffset + psiFile.text.lines()[line].length

            val detectionDetails = detection.detectionDetails
            val textRange = TextRange(startOffset, endOffset)

            if (!validateTextRange(textRange, psiFile) || !validateSastTextRange()) {
                return@forEach
            }

            val title = CycodeBundle.message("annotationTitle", detection.getFormattedTitle())

            val tooltip = CycodeBundle.message(
                "sastAnnotationTooltip",
                detection.severity,
                detectionDetails.policyDisplayName,
                detectionDetails.fileName,
            )
            holder.newAnnotation(severity, title)
                .range(textRange)
                .tooltip(tooltip)
                .withFix(CycodeOpenViolationCardIntentionQuickFix(detection))
                .create()
        }
    }
}
