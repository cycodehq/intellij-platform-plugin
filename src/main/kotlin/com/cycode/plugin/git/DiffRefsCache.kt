package com.cycode.plugin.git

/**
 * The revision lists together with the git state they were built for. Kept as one immutable object so a list can
 * never be paired with the fingerprint of a different state.
 */
data class DiffRefsCache(val fingerprint: String?, val refs: List<RefOption>) {
    /** Reusable only when git state is known, unchanged since the lists were built, and there is something to show. */
    fun canReuse(currentFingerprint: String?): Boolean {
        return currentFingerprint != null && currentFingerprint == fingerprint && refs.isNotEmpty()
    }
}
