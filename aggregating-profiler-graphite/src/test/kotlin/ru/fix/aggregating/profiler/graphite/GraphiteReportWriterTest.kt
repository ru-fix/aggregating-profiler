package ru.fix.aggregating.profiler.graphite

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.fix.aggregating.profiler.AggregatingProfiler
import ru.fix.aggregating.profiler.Identity
import ru.fix.aggregating.profiler.graphite.client.GraphiteEntity
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter

@ExtendWith(MockKExtension::class)
class GraphiteReportWriterTest {

    @MockK
    lateinit var writer: GraphiteWriter

    @Test
    fun `write report`() {


        val reportWriter = GraphiteReportWriter("prefix", writer)

        val profiler = AggregatingProfiler()
        val reporter = profiler.createReporter()

        profiler.attachIndicator("indicator1") { 42 }
        profiler.attachIndicator(Identity("indicator2", mapOf("type" to "hard"))) { 43 }

        profiler.profiledCall("call1").call()
        profiler.profiledCall(Identity("call2", mapOf("type" to "soft"))).call()


        val report = reporter.buildReportAndReset()


        val captureMetricPrefix = mutableListOf<String>()
        val captureMetricEntity = mutableListOf<List<GraphiteEntity>>()

        every {
            writer.write(capture(captureMetricPrefix), capture(captureMetricEntity))
        } answers {}

        reportWriter.saveProfilingReportToGraphite(report)


        captureMetricPrefix.forEach { Assert.assertThat(it, Matchers.equalTo("prefix")) }


        fun assertEntryExist(assert: GraphiteEntity.() -> Boolean) = captureMetricEntity
                .flatMap { it }
                .find { it.assert() }
                .let { Assert.assertNotNull(it) }

        assertEntryExist { name == "indicator1.indicatorMax" && value =="42" }
        assertEntryExist { name == "indicator1.indicatorMin" && value =="42" }
        assertEntryExist { name == "indicator2.type.hard.indicatorMax" && value =="43" }
        assertEntryExist { name == "indicator2.type.hard.indicatorMin" && value =="43" }
        assertEntryExist { name == "call1.stopSum" && value == "1" }
        assertEntryExist { name == "call2.type.soft.stopSum" && value == "1" }
    }
}