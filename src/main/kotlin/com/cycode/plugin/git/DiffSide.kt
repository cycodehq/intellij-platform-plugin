package com.cycode.plugin.git

/** One side of a diff scan: the local directory (uncommitted changes) or a revision (branch, tag or commit). */
sealed interface DiffSide {
    object Local : DiffSide

    data class Rev(val ref: String) : DiffSide
}
