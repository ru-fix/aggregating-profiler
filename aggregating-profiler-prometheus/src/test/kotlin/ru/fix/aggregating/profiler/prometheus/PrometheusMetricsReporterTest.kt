package ru.fix.aggregating.profiler.prometheus

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import ru.fix.aggregating.profiler.AggregatingProfiler

internal class PrometheusMetricsReporterTest {

    @Test
    fun buildReportAndReset() {
        val profiler = AggregatingProfiler()
        val reporter = PrometheusMetricsReporter(profiler.createReporter())

        profiler.attachIndicator("my.first.indicator") { 42 }
        profiler.call("simpleCall")


        val report = reporter.buildReportAndReset()
        assertThat(report, containsString("TYPE my_first_indicator_indicatorMax gauge"))
        assertThat(report, containsString("my_first_indicator_indicatorMax 42.0"))

        assertThat(report, containsString("simpleCall_callsCountSum 1.0"))

        println(report)
    }
}