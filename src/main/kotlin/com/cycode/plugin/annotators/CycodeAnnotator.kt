package com.cycode.plugin.annotators

import com.cycode.plugin.annotators.annotationAppliers.IacApplier
import com.cycode.plugin.annotators.annotationAppliers.ScaApplier
import com.cycode.plugin.annotators.annotationAppliers.SecretApplier
import com.cycode.plugin.services.ScanResultsService
import com.cycode.plugin.services.scanResults
import com.intellij.lang.ExternalLanguageAnnotators
import com.intellij.lang.Language
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
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
        applyAnnotationsForIac(psiFile, holder)
    }

    private fun applyAnnotationsForSecrets(psiFile: PsiFile, holder: AnnotationHolder) {
        SecretApplier(getScanResults(psiFile)).apply(psiFile, holder)
    }

    private fun applyAnnotationsForSca(psiFile: PsiFile, holder: AnnotationHolder) {
        ScaApplier(getScanResults(psiFile)).apply(psiFile, holder)
    }

    private fun applyAnnotationsForIac(psiFile: PsiFile, holder: AnnotationHolder) {
        IacApplier(getScanResults(psiFile)).apply(psiFile, holder)
    }
}
