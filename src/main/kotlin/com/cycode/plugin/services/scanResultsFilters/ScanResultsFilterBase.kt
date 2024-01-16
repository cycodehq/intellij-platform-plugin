package com.cycode.plugin.services.scanResultsFilters

abstract class ScanResultsFilterBase<T>(val scanResults: T) {
    fun exclude(byValue: String? = null, byPath: String? = null, byRuleId: String? = null) {
        if (byValue != null) {
            excludeByValue(byValue)
        }
        if (byPath != null) {
            excludeByPath(byPath)
        }
        if (byRuleId != null) {
            excludeByRuleId(byRuleId)
        }
    }

    abstract fun excludeByValue(value: String)

    abstract fun excludeByPath(path: String)

    abstract fun excludeByRuleId(ruleId: String)

    abstract fun getFilteredScanResults(): T
}