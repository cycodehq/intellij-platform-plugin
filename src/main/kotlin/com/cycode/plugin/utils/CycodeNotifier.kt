package com.cycode.plugin.utils

import com.cycode.plugin.CycodeBundle
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project


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
}
