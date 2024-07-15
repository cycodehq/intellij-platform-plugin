package com.cycode.plugin.sentry

import com.cycode.plugin.Consts
import io.sentry.Sentry
import io.sentry.protocol.User

object SentryInit {
    fun init() {
        Sentry.init { options ->
            options.dsn = Consts.SENTRY_DSN
            options.isDebug = Consts.SENTRY_DEBUG
            options.sampleRate = Consts.SENTRY_SAMPLE_RATE
            options.isEnableUncaughtExceptionHandler = Consts.SENTRY_ENABLE_UNCAUGHT_EXCEPTION_HANDLER
            options.release = Consts.SENTRY_RELEASE
            options.isSendDefaultPii = Consts.SENTRY_SEND_DEFAULT_PII
            options.serverName = ""
        }
    }

    fun setupScope(userId: String, tenantId: String) {
        Sentry.configureScope { scope ->
            scope.setTag("tenant_id", tenantId)
            scope.user = User().apply {
                id = userId
                data = mapOf("tenant_id" to tenantId)
            }
        }
    }
}
