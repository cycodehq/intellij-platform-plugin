package com.cycode.plugin.sentry

import com.cycode.plugin.Consts
import com.cycode.plugin.CycodeBundle
import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import java.awt.Component
import java.io.PrintWriter
import java.io.StringWriter


fun getStackTraceAsString(throwable: Throwable): String {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    return sw.toString()
}

class SentryErrorReporter : ErrorReportSubmitter() {
    override fun getReportActionText(): String {
        return CycodeBundle.message("reportActionButton")
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val mgr = DataManager.getInstance()
        val context = mgr.getDataContext(parentComponent)
        val project: Project? = CommonDataKeys.PROJECT.getData(context)

        object : Task.Backgroundable(project, CycodeBundle.message("sentryReporting"), false) {
            override fun run(indicator: ProgressIndicator) {
                for (ideaEvent in events) {
                    val event = SentryEvent()
                    event.level = SentryLevel.ERROR
                    event.release = Consts.SENTRY_RELEASE
                    event.throwable = ideaEvent.throwable
                    event.serverName = ""

                    event.extras = mapOf(
                        "message" to ideaEvent.message,
                        "additional_info" to additionalInfo,
                        "last_action" to IdeaLogger.ourLastActionId,
                        // before 2025.1 it will contain more useful information
                        // since 2025.1 throwable is close to the original
                        "stacktrace" to getStackTraceAsString(ideaEvent.throwable)
                    )

                    Sentry.captureEvent(event)
                }

                ApplicationManager.getApplication().invokeLater {
                    consumer.consume(
                        SubmittedReportInfo(
                            SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
                        )
                    )
                }
            }
        }.queue()

        return true
    }
}