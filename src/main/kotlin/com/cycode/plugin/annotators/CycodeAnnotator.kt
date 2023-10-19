package com.cycode.plugin.annotators

import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.intentions.CycodeIgnoreIntentionQuickFix
import com.cycode.plugin.intentions.CycodeIgnoreType
import com.cycode.plugin.services.scanResults
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
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

    private fun applyAnnotationsForSecrets(psiFile: PsiFile, holder: AnnotationHolder) {
        val latestScanResult = scanResults.secretsResults
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

            val detectedValue = psiFile.text.substring(textRange.startOffset, textRange.endOffset)

            val message = detection.message.replace("within '' repository", "")  // BE bug
            val title = "Cycode: ${detection.type}. $message"
            val tooltip = """<html>
                Severity: ${detection.severity}<br>
                ${detection.type}: $message<br>
                Rule ID: ${detection.detectionRuleId}<br>
                In file: ${detectionDetails.fileName}<br>
                Secret SHA: ${detectionDetails.sha512}
            </html>""".trimIndent()
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
