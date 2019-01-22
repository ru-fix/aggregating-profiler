package ru.fix.aggregating.profiler.prometheus

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
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

    @Test
    fun `reporter tags`() {
        val profiler = AggregatingProfiler()
        val reporter = PrometheusMetricsReporter(profiler.createReporter(), mapOf("serverId" to "3"))

        profiler.call("first")
        profiler.profiledCall(Identity("second", "one", "1")).call()
        profiler.profiledCall(Identity("third", "serverId", "7")).call()
        profiler.profiledCall(Identity("fourth", "one", "1", "serverId", "7")).call()

        val report = reporter.buildReportAndReset()
        assertThat(report, containsString("""first_startSum{serverId="3"} 1.0"""))
        assertThat(report, containsString("""second_startSum{one="1",serverId="3"} 1.0"""))
        assertThat(report, containsString("""third_startSum{serverId="3"} 1.0"""))
        assertThat(report, containsString("""fourth_startSum{one="1",serverId="3"} 1.0"""))
        println(report)
    }

    fun String.allIndexOf(substring: String): List<Int> {
        val result = mutableListOf<Int>()
        var index = -1

        do {
            index = this.indexOf(substring, index + 1)
            if (index >= 0) {
                result.add(index)
            }
        } while (index >= 0)
        return result
    }


    @Test
    fun `several metrics with same name`() {
        val profiler = AggregatingProfiler()
        val reporter = PrometheusMetricsReporter(profiler.createReporter(), mapOf("serverId" to "3"))

        profiler.profiledCall(Identity("my")).call()
        profiler.profiledCall(Identity("my", "one", "1")).call()
        profiler.profiledCall(Identity("my", "two", "2")).call()
        profiler.profiledCall(Identity("my", "three", "3")).call()

        val report = reporter.buildReportAndReset()
        assertThat(report, report.allIndexOf("""# TYPE my_startSum gauge""").size, equalTo(1))

        assertThat(report, report.allIndexOf("""my_startSum{serverId="3"} 1.0""").size, equalTo(1))
        assertThat(report, report.allIndexOf("""my_startSum{one="1",serverId="3"} 1.0""").size, equalTo(1))
        assertThat(report, report.allIndexOf("""my_startSum{serverId="3",two="2"} 1.0""").size, equalTo(1))
        assertThat(report, report.allIndexOf("""my_startSum{serverId="3",three="3"} 1.0""").size, equalTo(1))
        println(report)
    }
}