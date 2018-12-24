package ru.fix.aggregating.profiler.engine

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.fix.aggregating.profiler.PercentileSettings

class PercentileAccumulatorTest {

    @Test
    fun buildAndReset() {
        val accumulator = PercentileAccumulator(PercentileSettings().apply {
            percentiles = listOf(50, 90, 99)
        })

        listOf<Long>(
                600,
                600,
                600,
                600,
                600,
                600,
                1000,
                1000,
                1000,
                1000,
                1000,
                42_000
        ).forEach { accumulator.accumulate(it) }

        val report = accumulator.buildAndReset(42_000)

        Assertions.assertEquals(
                mapOf(
                        "p50" to 750L,
                        "p90" to 1000L,
                        "p99" to 60_000L
                ),
                report)
    }
}