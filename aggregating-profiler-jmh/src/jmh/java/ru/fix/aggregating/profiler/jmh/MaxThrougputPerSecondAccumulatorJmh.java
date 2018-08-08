package ru.fix.aggregating.profiler.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import ru.fix.aggregating.profiler.engine.MaxThroughputPerSecondAccumulator;

@State(Scope.Benchmark)
public class MaxThrougputPerSecondAccumulatorJmh {
    MaxThroughputPerSecondAccumulator accumulator = new MaxThroughputPerSecondAccumulator();

    @Benchmark
    public void timestamp_and_call() {
        accumulator.call(System.currentTimeMillis(), 1);
    }
}
