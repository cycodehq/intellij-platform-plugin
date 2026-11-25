package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.git.DiffSide
import com.cycode.plugin.git.GitCli
import com.cycode.plugin.git.HEAD_REF
import com.cycode.plugin.git.RefOption
import com.cycode.plugin.utils.CycodeNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicLong

/**
 * Everything about git for the Diff scan scope: whether the project is a git repository, the two selected sides and
 * the cached revision lists. All of it is per project and session-only.
 */
@Service(Service.Level.PROJECT)
class DiffRefsService(private val project: Project) {
    private val cliService = cli(project)
    private val refreshRequestId = AtomicLong()

    // per-repository and therefore session-only; persisting them globally leaks picks between projects
    @Volatile
    var from: DiffSide = DiffSide.Local
        private set

    @Volatile
    var to: DiffSide = DiffSide.Rev(HEAD_REF)
        private set

    // rebuilt on every refresh: git is fast enough with the capped lists, and it picks up refs created elsewhere
    @Volatile
    var refs: List<RefOption> = emptyList()
        private set

    // null until known; only an explicit "not a git repository" answer makes it false
    @Volatile
    var isGitRepository: Boolean? = null
        private set

    // the "git is not available" message is shown once per session
    @Volatile
    private var gitUnavailableNotified = false

    fun setSides(from: DiffSide, to: DiffSide) {
        this.from = from
        this.to = to
    }

    /** Checks in the background whether the project is a git repository; rebuilds the scan tab if that changed. */
    fun refreshGitRepositoryState() {
        val projectRoot = cliService.getProjectRootDirectory() ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val isRepository = GitCli(projectRoot).isGitRepository()
            if (!project.isDisposed && isRepository != isGitRepository) {
                isGitRepository = isRepository
                updateToolWindowState(project, reloadResults = false)
            }
        }
    }

    /**
     * Reloads the revision lists in the background and resets stale picks to the defaults.
     * A request id drops results of superseded requests.
     */
    fun refresh(onDone: (() -> Unit)? = null) {
        val projectRoot = cliService.getProjectRootDirectory() ?: return
        val requestId = refreshRequestId.incrementAndGet()
        val pinned = listOf(from, to).filterIsInstance<DiffSide.Rev>()

        ApplicationManager.getApplication().executeOnPooledThread {
            val git = GitCli(projectRoot)
            val refs = git.getAvailableRefs(pinned)

            // nothing but HEAD: git may be missing (a repository without commits would still answer --version)
            if (refs.size <= 1 && !gitUnavailableNotified && !git.isGitAvailable()) {
                gitUnavailableNotified = true
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("diffGitUnavailableNotification"))
            }

            if (requestId != refreshRequestId.get() || project.isDisposed) {
                return@executeOnPooledThread
            }

            val known = refs.map { it.value }.toSet()
            if (from is DiffSide.Rev && (from as DiffSide.Rev).ref !in known) from = DiffSide.Local
            if (to is DiffSide.Rev && (to as DiffSide.Rev).ref !in known) to = DiffSide.Rev(HEAD_REF)
            this.refs = refs

            if (onDone != null) {
                ApplicationManager.getApplication().invokeLater({ onDone() }, project.disposed)
            }
        }
    }
}
