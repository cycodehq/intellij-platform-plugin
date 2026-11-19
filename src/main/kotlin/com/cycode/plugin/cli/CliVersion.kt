package com.cycode.plugin.cli

/** Compares only the first three numeric segments; dev builds look like `0.0.1.dev498`. */
fun isVersionAtLeast(version: String, minVersion: String): Boolean {
    fun parse(value: String) = value.trim().removePrefix("v").split(".").take(3)
        .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        .let { it + List(3 - it.size) { 0 } }

    val actual = parse(version)
    val required = parse(minVersion)
    for (i in 0 until 3) {
        if (actual[i] != required[i]) {
            return actual[i] > required[i]
        }
    }

    return true
}
