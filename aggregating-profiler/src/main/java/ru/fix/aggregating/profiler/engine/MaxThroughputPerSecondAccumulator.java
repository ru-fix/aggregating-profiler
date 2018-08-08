package ru.fix.aggregating.profiler.engine;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class MaxThroughputPerSecondAccumulator {
    private static final long ONE_SECOND_MS = 1_000;

    private final LongAccumulator maxEventCountPerSecond = new LongAccumulator(Math::max, 0L);
    private final LongAdder eventCountSum = new LongAdder();
    private final AtomicLong startOfSecondTimestamp = new AtomicLong();

    public void call(long currentTimestamp, long eventCount) {
        long start = startOfSecondTimestamp.get();
        if (start + ONE_SECOND_MS <= currentTimestamp) {
            if (startOfSecondTimestamp.compareAndSet(start, currentTimestamp)) {
                long sum = LongAdderDrainer.drain(eventCountSum);
                maxEventCountPerSecond.accumulate(sum);
            }
        }
        eventCountSum.add(eventCount);
    }

    public long getAndReset(long currentTimestamp) {
        //update counters before report return
        call(currentTimestamp, 0);
        return maxEventCountPerSecond.getThenReset();
    }
}

