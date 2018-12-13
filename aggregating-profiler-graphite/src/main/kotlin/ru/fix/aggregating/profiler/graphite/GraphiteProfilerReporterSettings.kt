package ru.fix.aggregating.profiler.graphite

import ru.fix.aggregating.profiler.graphite.client.GraphiteSettings

data class GraphiteProfilerReporterSettings(
        val graphiteSettings: GraphiteSettings = GraphiteSettings(),
        val selectiveRateProfilingConfig: SelectiveRateProfilingConfig = SelectiveRateProfilingConfig()
)


