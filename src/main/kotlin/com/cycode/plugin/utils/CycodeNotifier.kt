package com.cycode.plugin.utils

import com.cycode.plugin.CycodeBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project


object CycodeNotifier {
    fun notifyError(project: Project?, content: String?) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(CycodeBundle.message("notificationGroupId"))
            .createNotification(CycodeBundle.message("notificationTitle"), content!!, NotificationType.ERROR)
            .notify(project)
    }
}
