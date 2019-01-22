package ru.fix.aggregating.profiler.engine

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.fix.aggregating.profiler.PercentileSettings

class PercentileAccumulatorTest {

    @Test
    fun `metrics values within buckets boundaries`() {
        val accumulator = PercentileAccumulator(PercentileSettings().apply {
            percentiles = listOf(50, 90, 99)
            buckets = listOf(
                    250,
                    500,
                    750,
                    1000,
                    1500,
                    3000,
                    6000,
                    30000,
                    60000
            )
        })

        val measurements = listOf<Long>(
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
        )
        measurements.forEach { accumulator.accumulate(it) }

        val report = accumulator.buildAndReset(measurements.max()!!, measurements.size.toLong())

        Assertions.assertEquals(
                mapOf(
                        50 to 750L,
                        90 to 1000L,
                        99 to 60_000L
                ),
                report)
    }

    @Test
    fun `bucket is big enough so we fill two percentiles in report with same value`() {
        val accumulator = PercentileAccumulator(PercentileSettings().apply {
            percentiles = listOf(90, 94, 99)
            buckets = listOf(
                    10,
                    20,
                    30,
                    40,
                    50
            )
        })

        //  total 200
        //  p90 - 180 measurements
        //  p94 - 188 measurements
        //  p99 - 198 measurements

        val measurements = mutableListOf<Long>()
        repeat(140) { measurements.add(5) } //total 140
        repeat(50) { measurements.add(22) } //total 190
        repeat(10) { measurements.add(41) } //total 200

        measurements.forEach { accumulator.accumulate(it) }

        val report = accumulator.buildAndReset(measurements.max()!!, measurements.size.toLong())

        Assertions.assertEquals(
                mapOf(
                        90 to 30L,
                        94 to 30L,
                        99 to 50L
                ),
                report)
    }

    @Test
    fun `metrics value out of bucket boundaries`() {
        val accumulator = PercentileAccumulator(PercentileSettings().apply {
            percentiles = listOf(50, 90, 99)
            buckets = listOf(
                    250,
                    500,
                    750,
                    1000,
                    1500,
                    3000
            )
        })

        val measurements = listOf<Long>(
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
        )
        measurements.forEach { accumulator.accumulate(it) }

        val report = accumulator.buildAndReset(measurements.max()!!, measurements.size.toLong())

        Assertions.assertEquals(
                mapOf(
                        50 to 750L,
                        90 to 1000L,
                        99 to 42_000L
                ),
                report)
    }
}