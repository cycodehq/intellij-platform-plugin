package com.cycode.plugin.annotators

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

fun convertSeverity(severity: String): HighlightSeverity {
    return when (severity.lowercase()) {
        "critical" -> HighlightSeverity.ERROR
        "high" -> HighlightSeverity.ERROR
        "medium" -> HighlightSeverity.WARNING
        "low" -> HighlightSeverity.WEAK_WARNING
        else -> HighlightSeverity.INFORMATION
    }
}

fun validateTextRange(textRange: TextRange, psiFile: PsiFile): Boolean {
    // check if text range fits in file
    // case: row with detections has been deleted, but detection is still in the local results DB
    return !(textRange.endOffset > psiFile.text.length || textRange.startOffset < 0)
}
