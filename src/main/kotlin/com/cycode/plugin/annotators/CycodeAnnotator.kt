package com.cycode.plugin.annotators

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.intentions.CycodeIgnoreIntentionQuickFix
import com.cycode.plugin.intentions.CycodeIgnoreType
import com.cycode.plugin.services.scanResults
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class CycodeAnnotator : DumbAware, ExternalAnnotator<PsiFile, Unit>() {
    private val scanResults = scanResults()

    override fun collectInformation(file: PsiFile): PsiFile = file
    override fun doAnnotate(psiFile: PsiFile?) {}

    override fun apply(psiFile: PsiFile, annotationResult: Unit, holder: AnnotationHolder) {
        applyAnnotationsForSecrets(psiFile, holder)
        applyAnnotationsForSca(psiFile, holder)
    }

    private fun convertSeverity(severity: String): HighlightSeverity {
        return when (severity.toLowerCase()) {
            "critical" -> HighlightSeverity.ERROR
            "high" -> HighlightSeverity.ERROR
            "medium" -> HighlightSeverity.WARNING
            "low" -> HighlightSeverity.WEAK_WARNING
            else -> HighlightSeverity.INFORMATION
        }
    }

    private fun validateTextRange(scanType: CliScanType, textRange: TextRange, psiFile: PsiFile): Boolean {
        if (textRange.endOffset > psiFile.text.length || textRange.startOffset < 0) {
            // check if text range fits in file

            // case: row with detections has been deleted, but detection is still in the local results DB
            thisLogger().warn("Text range of detection is out of file bounds")
            return false
        }

        val detectedSegment = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
        if (scanResults.getDetectedSegment(scanType, textRange) == null) {
            scanResults.saveDetectedSegment(scanType, textRange, detectedSegment)
        } else if (scanResults.getDetectedSegment(scanType, textRange) != detectedSegment) {
            // case: the code has been added or deleted before the detection
            thisLogger().warn(
                "Text range of detection has been shifted. " +
                        "Annotation is not relevant to this state of the file content anymore"
            )
            return false
        }

        return true
    }

    private fun applyAnnotationsForSecrets(psiFile: PsiFile, holder: AnnotationHolder) {
        val latestScanResult = scanResults.getSecretsResults()
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

            if (!validateTextRange(CliScanType.Secret, textRange, psiFile)) {
                return@forEach
            }

            val detectedValue = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
            val message = detection.message.replace("within '' repository", "")  // BE bug
            val title = CycodeBundle.message("secretsAnnotationTitle", detection.type, message)
            val tooltip = CycodeBundle.message(
                "secretsAnnotationTooltip",
                detection.severity,
                detection.type,
                message,
                detection.detectionRuleId,
                detectionDetails.fileName,
                detectionDetails.sha512
            )
            holder.newAnnotation(severity, title)
                .range(textRange)
                .tooltip(tooltip)
                .withFix(CycodeIgnoreIntentionQuickFix(CycodeIgnoreType.PATH, detection.detectionDetails.getFilepath()))
                .withFix(CycodeIgnoreIntentionQuickFix(CycodeIgnoreType.RULE, detection.detectionRuleId))
                .withFix(CycodeIgnoreIntentionQuickFix(CycodeIgnoreType.VALUE, detectedValue))
                .create()

        }
    }

    private fun applyAnnotationsForSca(psiFile: PsiFile, holder: AnnotationHolder) {}
}
