package com.cycode.plugin.utils

import com.cycode.plugin.CycodeBundle
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager


class OpenProblemsTabAction : AnAction(CycodeBundle.message("openProblemsTabAction")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(CycodeBundle.message("toolWindowId"))
        toolWindow?.activate(null)
    }
}


object CycodeNotifier {
    private fun getNotificationGroup(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup(CycodeBundle.message("notificationGroupId"))
    }

    fun notifyError(project: Project?, content: String?) {
        getNotificationGroup()
            .createNotification(CycodeBundle.message("notificationTitle"), content!!, NotificationType.ERROR)
            .notify(project)
    }

    fun notifyInfo(project: Project?, content: String?) {
        getNotificationGroup()
            .createNotification(CycodeBundle.message("notificationTitle"), content!!, NotificationType.INFORMATION)
            .notify(project)
    }

    fun notifyDetections(project: Project?, content: String?) {
        getNotificationGroup()
            .createNotification(CycodeBundle.message("notificationTitle"), content!!, NotificationType.INFORMATION)
            .addAction(OpenProblemsTabAction())
            .notify(project)
    }
}
