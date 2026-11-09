package com.cycode.plugin.services

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.git.DiffRefsCache
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

    // the revision lists and the git state they were built for (see GitCli.getHeadFingerprint)
    @Volatile
    private var cache: DiffRefsCache? = null

    val refs: List<RefOption>
        get() = cache?.refs ?: emptyList()

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
                updateToolWindowState(project)
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
        val pinned = listOf(from, to).filterIsInstance<DiffSide.Rev>().map { it.ref }

        ApplicationManager.getApplication().executeOnPooledThread {
            val git = GitCli(projectRoot)

            // the lists are rebuilt only when the branch changes or a commit is created
            val fingerprint = git.getHeadFingerprint()
            if (fingerprint == null && !gitUnavailableNotified && !git.isGitAvailable()) {
                gitUnavailableNotified = true
                CycodeNotifier.notifyInfo(project, CycodeBundle.message("diffGitUnavailableNotification"))
            }

            val current = cache
            val reuse = current != null && current.canReuse(fingerprint)
            val refs = if (reuse) current!!.refs else git.getAvailableRefs(pinned)
            if (requestId != refreshRequestId.get() || project.isDisposed) {
                return@executeOnPooledThread
            }

            val known = refs.map { it.value }.toSet()
            if (from is DiffSide.Rev && (from as DiffSide.Rev).ref !in known) from = DiffSide.Local
            if (to is DiffSide.Rev && (to as DiffSide.Rev).ref !in known) to = DiffSide.Rev(HEAD_REF)
            // only a request that is still current may publish lists, together with the state they were built for
            if (!reuse) cache = DiffRefsCache(fingerprint, refs)

            if (onDone != null) {
                ApplicationManager.getApplication().invokeLater({ onDone() }, project.disposed)
            }
        }
    }
}
