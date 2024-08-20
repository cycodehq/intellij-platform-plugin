package com.cycode.plugin.listeners

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame

internal class ApplicationActivationListener :
    ApplicationActivationListener {

    override fun applicationActivated(ideFrame: IdeFrame) {}
}
