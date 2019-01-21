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
        assertThat(report, containsString("TYPE my_first_indicator gauge"))
        assertThat(report, containsString("my_first_indicator 42.0"))

        assertThat(report, containsString("simpleCall_stopSum 1.0"))
        println(report)
    }

    @Test
    fun `profiled call and indicator with tags`() {
        val profiler = AggregatingProfiler()
        val reporter = PrometheusMetricsReporter(profiler.createReporter())

        profiler.attachIndicator(Identity("my.first.indicator", mapOf("type" to "soft"))) { 42 }
        profiler.profiledCall(Identity("simpleCall", mapOf("type" to "hard", "rate" to "fast"))).call()


        val report = reporter.buildReportAndReset()
        assertThat(report, containsString("TYPE my_first_indicator gauge"))
        assertThat(report, containsString("""my_first_indicator{type="soft"} 42.0"""))

        assertThat(report, containsString("""simpleCall_stopSum{rate="fast",type="hard"} 1.0"""))
        println(report)
    }

    @Test
    fun `convert wrong names`() {
        val profiler = AggregatingProfiler()
        val reporter = PrometheusMetricsReporter(profiler.createReporter())

        profiler.call("my.simple.call")
        profiler.call("my other invocation")
        profiler.call("percentile98")
        profiler.call("wrong-metric.name")
        profiler.call("another.name.")


        val report = reporter.buildReportAndReset()
        assertThat(report, containsString("my_simple_call_stopSum 1.0"))
        assertThat(report, containsString("my_other_invocation_stopSum 1.0"))
        assertThat(report, containsString("percentile98_stopSum 1.0"))
        assertThat(report, containsString("wrong_metric_name_stopSum 1.0"))
        assertThat(report, containsString("another_name_stopSum 1.0"))
        println(report)
    }
}