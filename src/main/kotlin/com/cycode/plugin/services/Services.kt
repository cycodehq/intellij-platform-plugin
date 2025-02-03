package com.cycode.plugin.services

import com.intellij.openapi.components.ServiceManager.getService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project


inline fun <reified T : Any> getCycodeService(project: Project? = null): T {
    if (project != null) {
        return project.service<T>()
    }

    return getService(T::class.java)
}

fun pluginState(project: Project? = null): CycodePersistentStateService = getCycodeService(project)

fun pluginLocalState(project: Project? = null): CycodeTemporaryStateService = getCycodeService(project)

fun pluginSettings(project: Project? = null): CycodePersistentSettingsService = getCycodeService(project)

fun scanResults(project: Project? = null): ScanResultsService = getCycodeService(project)

fun cli(project: Project? = null): CliService = getCycodeService(project)

fun cliDownload(project: Project? = null): CliDownloadService = getCycodeService(project)

fun download(project: Project? = null): DownloadService = getCycodeService(project)

fun githubReleases(project: Project? = null): GithubReleaseService = getCycodeService(project)

fun cycode(project: Project? = null): CycodeService = getCycodeService(project)
