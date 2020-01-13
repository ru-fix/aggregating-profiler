package ru.fix.aggregating.profiler.engine

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.fix.aggregating.profiler.AggregatingProfiler
import ru.fix.aggregating.profiler.Identity

class ActiveCallsLimitTest {

    private val numberOfActiveCalls = 25

    private lateinit var profiler: AggregatingProfiler
    private lateinit var reporter: AggregatingReporter

    @BeforeEach
    fun setup() {
        profiler = AggregatingProfiler().setNumberOfActiveCallsToTrackAndKeepBetweenReports(numberOfActiveCalls)
        reporter = profiler.createReporter() as AggregatingReporter
    }

    @AfterEach
    fun tearDown() {
        reporter.close()
    }

    @Test
    fun `start profiling concurrently and count active calls`() {
        for (jobs in 1 .. 1000) {
            GlobalScope.launch {
                for (metrics in 1 .. 100) {
                    profiler.start("Test")
                }
            }
        }

        runBlocking {
            delay(1000)
        }

        reporter.buildReportAndReset()

        reporter.updateCallAggregates(Identity("Test")) {counters ->
            assertTrue(counters.activeCalls.size >= numberOfActiveCalls)
            assertTrue(counters.activeCalls.size < numberOfActiveCalls * 2)
        }
    }
}