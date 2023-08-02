package com.cycode.plugin.services

import com.intellij.openapi.components.ServiceManager.getService


inline fun <reified T : Any> getCycodeService(): T = getService(T::class.java)

fun pluginState(): CycodePersistentStateService = getCycodeService()
