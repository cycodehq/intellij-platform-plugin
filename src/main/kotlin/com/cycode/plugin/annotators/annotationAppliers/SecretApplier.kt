package com.cycode.plugin.annotators.annotationAppliers

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.annotators.convertSeverity
import com.cycode.plugin.annotators.validateTextRange
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.intentions.CycodeIgnoreIntentionQuickFix
import com.cycode.plugin.intentions.CycodeIgnoreType
import com.cycode.plugin.services.ScanResultsService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class SecretApplier(private val scanResults: ScanResultsService) : AnnotationApplierBase() {
    private fun validateSecretTextRange(textRange: TextRange, psiFile: PsiFile): Boolean {
        val detectedSubstr = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
        val detectedSegment = scanResults.getDetectedSegment(CliScanType.Secret, textRange)
        if (detectedSegment == null) {
            scanResults.saveDetectedSegment(CliScanType.Secret, textRange, detectedSubstr)
        } else if (detectedSegment != detectedSubstr) {
            // case: the code has been added or deleted before the detection
            thisLogger().debug(
                "[Secret] Text range of detection has been shifted. " +
                        "Annotation is not relevant to this state of the file content anymore"
            )
            return false
        }

        return true
    }

    override fun apply(psiFile: PsiFile, holder: AnnotationHolder) {
        val latestScanResult = scanResults.getSecretResults()
        if (latestScanResult !is CliResult.Success) {
            return
        }

        val relevantDetections = latestScanResult.result.detections.filter { detection ->
            detection.detectionDetails.getFilepath() == psiFile.virtualFile.path
        }

        relevantDetections.forEach { detection ->
            val severity = convertSeverity(detection.severity)

            val detectionDetails = detection.detectionDetails
            val textRange = TextRange(
                detectionDetails.startPosition,
                detectionDetails.startPosition + detectionDetails.length
            )

            if (!validateTextRange(textRange, psiFile) || !validateSecretTextRange(textRange, psiFile)) {
                return@forEach
            }

            val detectedValue = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
            detectionDetails.detectedValue = detectedValue

            val message = detection.getFormattedMessage()
            val title = CycodeBundle.message("annotationTitle", detection.getFormattedTitle())

            val tooltip = CycodeBundle.message(
                "secretsAnnotationTooltip",
                detection.severity,
                detection.type,
                message,
                detectionDetails.fileName,
                detectionDetails.sha512,
            )
            holder.newAnnotation(severity, title)
                .range(textRange)
                .tooltip(tooltip)
                .withFix(
                    CycodeIgnoreIntentionQuickFix(
                        CliScanType.Secret,
                        CycodeIgnoreType.PATH,
                        detection.detectionDetails.getFilepath()
                    )
                )
                .withFix(
                    CycodeIgnoreIntentionQuickFix(
                        CliScanType.Secret,
                        CycodeIgnoreType.RULE,
                        detection.detectionRuleId
                    )
                )
                .withFix(CycodeIgnoreIntentionQuickFix(CliScanType.Secret, CycodeIgnoreType.VALUE, detectedValue))
                .create()

        }
    }
}
