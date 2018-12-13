package ru.fix.aggregating.profiler.prometheus

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import ru.fix.aggregating.profiler.AggregatingProfiler
import ru.fix.aggregating.profiler.Identity

internal class PrometheusMetricsReporterTest {

    @Test
    fun `profiled call and indicator`() {
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

    @Test
    fun `profiled call and indicator with tags`() {
        val profiler = AggregatingProfiler()
        val reporter = PrometheusMetricsReporter(profiler.createReporter())

        profiler.attachIndicator(Identity("my.first.indicator", mapOf("type" to "soft"))) { 42 }
        profiler.profiledCall(Identity("simpleCall", mapOf("type" to "hard", "rate" to "fast"))).call()


        val report = reporter.buildReportAndReset()
        assertThat(report, containsString("TYPE my_first_indicator_indicatorMax gauge"))
        assertThat(report, containsString("""my_first_indicator_indicatorMax{type="soft"} 42.0"""))

        assertThat(report, containsString("""simpleCall_callsCountSum 1.0{type="hard",rate="fast"}"""))
        println(report)
    }
}