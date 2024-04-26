package com.cycode.plugin.annotators.annotationAppliers

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.annotators.convertSeverity
import com.cycode.plugin.annotators.validateTextRange
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getPackageFileForLockFile
import com.cycode.plugin.cli.isSupportedLockFile
import com.cycode.plugin.intentions.CycodeIgnoreIntentionQuickFix
import com.cycode.plugin.intentions.CycodeIgnoreType
import com.cycode.plugin.services.ScanResultsService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class ScaApplier(private val scanResults: ScanResultsService) : AnnotationApplierBase() {
    private fun validateScaTextRange(textRange: TextRange, psiFile: PsiFile, expectedPackageName: String): Boolean {
        // text range is dynamic and calculated from the line number,
        // so we can't use the same validation as for secrets
        // instead, we check if the package name is still in the text range
        val detectedSubstr = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
        if (!detectedSubstr.contains(expectedPackageName)) {
            thisLogger().debug(
                "[SCA] Text range of detection has been shifted. " +
                        "Annotation is not relevant to this state of the file content anymore"
            )
            return false
        }

        return true
    }

    override fun apply(psiFile: PsiFile, holder: AnnotationHolder) {
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

            val title = CycodeBundle.message("annotationTitle", detection.getFormattedTitle())

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
                lockFileNote,
            )
            holder.newAnnotation(severity, title)
                .range(textRange)
                .tooltip(tooltip)
                .withFix(
                    CycodeIgnoreIntentionQuickFix(
                        CliScanType.Sca,
                        CycodeIgnoreType.PATH,
                        detection.detectionDetails.getFilepath()
                    )
                )
                .withFix(
                    CycodeIgnoreIntentionQuickFix(
                        CliScanType.Sca,
                        CycodeIgnoreType.RULE,
                        detection.detectionRuleId
                    )
                )
                .create()
        }
    }
}
