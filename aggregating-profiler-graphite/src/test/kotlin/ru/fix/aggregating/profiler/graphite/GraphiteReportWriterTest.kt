package ru.fix.aggregating.profiler.graphite

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.fix.aggregating.profiler.AggregatingProfiler
import ru.fix.aggregating.profiler.Identity
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

        reportWriter.saveProfilingReportToGraphite(report)


        verify {  }

    }
}