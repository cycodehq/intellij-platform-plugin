package com.cycode.plugin.git

import com.cycode.plugin.cli.assertSafeRefArg

/** One side of a diff scan: the local directory (uncommitted changes) or a revision (branch, tag or commit). */
sealed interface DiffSide {
    object Local : DiffSide

    /** The only place a revision enters the model: an unsafe value (e.g. `--output=x`) can never get further. */
    data class Rev(val ref: String) : DiffSide {
        init {
            assertSafeRefArg(ref)
        }
    }
}
