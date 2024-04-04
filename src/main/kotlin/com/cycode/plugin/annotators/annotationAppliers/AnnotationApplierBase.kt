package com.cycode.plugin.annotators.annotationAppliers

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiFile

abstract class AnnotationApplierBase {
    abstract fun apply(psiFile: PsiFile, holder: AnnotationHolder)
}
