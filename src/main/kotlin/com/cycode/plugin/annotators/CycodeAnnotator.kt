package com.cycode.plugin.annotators

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliResult
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.getPackageFileForLockFile
import com.cycode.plugin.cli.isSupportedLockFile
import com.cycode.plugin.intentions.CycodeIgnoreIntentionQuickFix
import com.cycode.plugin.intentions.CycodeIgnoreType
import com.cycode.plugin.services.ScanResultsService
import com.cycode.plugin.services.scanResults
import com.intellij.lang.ExternalLanguageAnnotators
import com.intellij.lang.Language
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

private val LOG = logger<CycodeAnnotator>()

class CycodeAnnotator : DumbAware, ExternalAnnotator<PsiFile, Unit>() {
    private val registeredForLanguages = mutableSetOf<Language>()
    private val psiFileCallsCounter = mutableMapOf<PsiFile, Int>()

    private fun getScanResults(psiFile: PsiFile): ScanResultsService {
        return scanResults(psiFile.project)
    }

    companion object {
        val INSTANCE: CycodeAnnotator by lazy {
            CycodeAnnotator()
        }
    }

    override fun collectInformation(file: PsiFile): PsiFile = file
    override fun doAnnotate(psiFile: PsiFile?) {}

    fun registerForAllLangs() {
        /**
         * we want to annotate all languages because secrets can be detected in any file
         * and SCA can be detected in any supported package and lock file.
         * maintaining a list of supported languages is not flexible enough
         * because of non JetBrains plugins
         */
        Language.getRegisteredLanguages().forEach {
            if (registeredForLanguages.contains(it)) {
                return@forEach
            }

            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(it, this)
            registeredForLanguages.add(it)
        }
    }

    private fun countExpectedApplyCalls(psiFile: PsiFile): Int {
        var expectedCallsCount = 0

        val viewProvider = psiFile.viewProvider
        for (language in viewProvider.languages) {
            val psiRoot = viewProvider.getPsi(language)
            val annotators = ExternalLanguageAnnotators.allForFile(language, psiRoot)
            expectedCallsCount += annotators.filterIsInstance<CycodeAnnotator>().count()
        }

        return expectedCallsCount
    }

    private fun getCallCount(psiFile: PsiFile): Int {
        synchronized(psiFileCallsCounter) {
            if (psiFileCallsCounter.containsKey(psiFile)) {
                return psiFileCallsCounter[psiFile]!!
            }
        }

        return 0
    }

    private fun incrementCallCount(psiFile: PsiFile) {
        synchronized(psiFileCallsCounter) {
            val callCount = getCallCount(psiFile)
            psiFileCallsCounter[psiFile] = callCount + 1
        }
    }

    private fun resetCallCount(psiFile: PsiFile) {
        synchronized(psiFileCallsCounter) {
            // we are deleting instead of setting to 0 to save memory
            psiFileCallsCounter.remove(psiFile)
        }
    }

    private fun shouldIgnoreApplyCall(psiFile: PsiFile): Boolean {
        /**
         * Our annotator is registered for all available languages.
         * One file could contain multiple languages.
         * Also, it's possible that multiple languages are registered for the same file extension or mime type.
         * As a result, our annotator will be called multiple times for the same file, but for different languages.
         * Our goal is to ignore all calls except one (any of them) to prevent duplications.
         * In the current implementation, we are applying annotations only on the last call.
         * This method counts calls and returns true if we should ignore the current one.
         */

        incrementCallCount(psiFile)

        if (getCallCount(psiFile) < countExpectedApplyCalls(psiFile)) {
            // we are waiting for all annotators to trigger
            LOG.debug("Ignore this apply. Calls ${getCallCount(psiFile)}/${countExpectedApplyCalls(psiFile)}")
            return true
        }

        LOG.debug("Apply called. This is the last call.")
        resetCallCount(psiFile)
        return false
    }

    override fun apply(psiFile: PsiFile, annotationResult: Unit, holder: AnnotationHolder) {
        if (shouldIgnoreApplyCall(psiFile)) {
            return
        }

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
        val scanResults = getScanResults(psiFile)
        val detectedSubstr = psiFile.text.substring(textRange.startOffset, textRange.endOffset)
        val detectedSegment = scanResults.getDetectedSegment(CliScanType.Secret, textRange)
        if (detectedSegment == null) {
            scanResults.saveDetectedSegment(CliScanType.Secret, textRange, detectedSubstr)
        } else if (detectedSegment != detectedSubstr) {
            // case: the code has been added or deleted before the detection
            LOG.debug(
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
            LOG.debug(
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
            LOG.debug("Text range of detection is out of file bounds")
            return false
        }

        return true
    }

    private fun applyAnnotationsForSecrets(psiFile: PsiFile, holder: AnnotationHolder) {
        val scanResults = getScanResults(psiFile)
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

    private fun applyAnnotationsForSca(psiFile: PsiFile, holder: AnnotationHolder) {
        val scanResults = getScanResults(psiFile)
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
                detection.detectionRuleId,
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
