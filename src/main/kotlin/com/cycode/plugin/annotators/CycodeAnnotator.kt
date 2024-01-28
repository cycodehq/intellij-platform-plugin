package com.cycode.plugin.annotators

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getPackageFileForLockFile
import com.cycode.plugin.cli.isSupportedLockFile
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

    private fun validateSecretTextRange(textRange: TextRange, psiFile: PsiFile): Boolean {
        val detectedSubstr = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
        val detectedSegment = scanResults.getDetectedSegment(CliScanType.Secret, textRange)
        if (detectedSegment == null) {
            scanResults.saveDetectedSegment(CliScanType.Secret, textRange, detectedSubstr)
        } else if (detectedSegment != detectedSubstr) {
            // case: the code has been added or deleted before the detection
            thisLogger().warn(
                "[Secret] Text range of detection has been shifted. " +
                        "Annotation is not relevant to this state of the file content anymore"
            )
            return false
        }

        return true
    }

    private fun validateScaTextRange(textRange: TextRange, psiFile: PsiFile, expectedPackageName: String): Boolean {
        // text range is dynamic and calculated from the line number,
        // so we can't use the same validation as for secrets
        // instead, we check if the package name is still in the text range
        val detectedSubstr = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
        if (!detectedSubstr.contains(expectedPackageName)) {
            thisLogger().warn(
                "[SCA] Text range of detection has been shifted. " +
                        "Annotation is not relevant to this state of the file content anymore"
            )
            return false
        }

        return true
    }

    private fun validateTextRange(textRange: TextRange, psiFile: PsiFile): Boolean {
        if (textRange.endOffset > psiFile.text.length || textRange.startOffset < 0) {
            // check if text range fits in file

            // case: row with detections has been deleted, but detection is still in the local results DB
            thisLogger().warn("Text range of detection is out of file bounds")
            return false
        }

        return true
    }

    private fun applyAnnotationsForSecrets(psiFile: PsiFile, holder: AnnotationHolder) {
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

            val message = detection.message.replace("within '' repository", "")  // BE bug
            val title = CycodeBundle.message("secretsAnnotationTitle", detection.type, message)

            var companyGuidelineMessage = ""
            if (detectionDetails.customRemediationGuidelines != null) {
                companyGuidelineMessage = CycodeBundle.message(
                    "secretsAnnotationTooltipCompanyGuideline",
                    detectionDetails.customRemediationGuidelines
                )
            }

            val tooltip = CycodeBundle.message(
                "secretsAnnotationTooltip",
                detection.severity,
                detection.type,
                message,
                detection.detectionRuleId,
                detectionDetails.fileName,
                detectionDetails.sha512,
                companyGuidelineMessage
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

    private fun applyAnnotationsForSca(psiFile: PsiFile, holder: AnnotationHolder) {
        val latestScanResult = scanResults.getScaResults()
        if (latestScanResult !is CliResult.Success) {
            return
        }

        val relevantDetections = latestScanResult.result.detections.filter { detection ->
            detection.detectionDetails.getFilepath() == psiFile.virtualFile.path
        }

        relevantDetections.forEach { detection ->
            val severity = convertSeverity(detection.severity)

            // SCA doesn't provide start and end positions, so we have to calculate them from the line number
            val line = detection.detectionDetails.lineInFile - 1
            val startOffset = psiFile.text.lines().take(line).sumOf { it.length + 1 }
            val endOffset = startOffset + psiFile.text.lines()[line].length

            val detectionDetails = detection.detectionDetails
            val textRange = TextRange(startOffset, endOffset)

            if (!validateTextRange(textRange, psiFile) || !validateScaTextRange(
                    textRange,
                    psiFile,
                    detectionDetails.packageName
                )
            ) {
                return@forEach
            }

            val title = CycodeBundle.message(
                "scaAnnotationTitle",
                detectionDetails.packageName,
                detectionDetails.packageVersion,
                // using the message as fallback for non-premise license detections
                detectionDetails.vulnerabilityDescription ?: detection.message
            )

            var firstPatchedVersionMessage = ""
            if (detectionDetails.alert?.firstPatchedVersion != null) {
                firstPatchedVersionMessage = CycodeBundle.message(
                    "scaAnnotationTooltipFirstPatchedVersion",
                    detectionDetails.alert.firstPatchedVersion
                )
            }

            var lockFileNote = ""
            if (isSupportedLockFile(psiFile.virtualFile.name)) {
                val packageFileName = getPackageFileForLockFile(psiFile.virtualFile.name)
                lockFileNote = CycodeBundle.message("scaAnnotationTooltipLockFileNote", packageFileName)
            }

            val tooltip = CycodeBundle.message(
                "scaAnnotationTooltip",
                detection.severity,
                firstPatchedVersionMessage,
                detection.message,
                detection.detectionRuleId,
                lockFileNote,
            )
            holder.newAnnotation(severity, title)
                .range(textRange)
                .tooltip(tooltip)
                // TODO(MarshalX): add quick fix for SCA (ignoring)
                .create()
        }
    }
}
