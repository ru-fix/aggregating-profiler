package ru.fix.aggregating.profiler.graphite

/**
 * Example:
 * ```
 * {
 *    "defaultTimeout": 60000,
 *    "reportingSchedule": {
 *        "80000": [".*HBase_scanHasNext.*"]
 *    }
 * }
 * ```
 * By default all metrics will be reported to graphite each 60_000 ms.
 * Metrics that contains HBase_scanHasNext in their names will be reported each 80_000 ms.
 */
data class SelectiveRateProfilingConfig(

        /**
         * By default defaultTimeout will be used as reporting period for Graphite profiler reporter
         * Each defaultTimeout milliseconds profiler reports will be set to graphite.
         */
        val defaultTimeout: Long = 60_000L,

        /**
         * Specifies how often particular metrics will be reported.
         * List of regexp strings will be used to select metrics by their names.
         */
        val reportingSchedule: MutableMap<Int, List<String>> = mutableMapOf(),

        val enableReporting: Boolean = true
)