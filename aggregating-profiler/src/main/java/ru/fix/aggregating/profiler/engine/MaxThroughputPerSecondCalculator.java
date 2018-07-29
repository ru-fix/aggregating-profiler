package ru.fix.aggregating.profiler.engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Gleb Beliaev
 * Created 11.01.18.
 */
public class MaxThroughputPerSecondCalculator {
    private static final long ONE_SECOND_MS = 1_000;

    private final AtomicLong maxCallCountPerSecond = new AtomicLong();
    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong timeBeginningOfSecond = new AtomicLong();

    public void call() {
        call(1);
    }

    public void call(long eventCount) {
        long count = callCount.getAndAdd(eventCount);
        long start = timeBeginningOfSecond.get();
        long now = currentTimeMillis();

        if (start + ONE_SECOND_MS <= now) {
            if (timeBeginningOfSecond.compareAndSet(start, now)) {
                callCount.addAndGet(-count);

                boolean maxUpdateSucceed;
                do {
                    long currentMax = maxCallCountPerSecond.get();
                    maxUpdateSucceed = count > currentMax && !maxCallCountPerSecond.compareAndSet(currentMax, count);
                } while (maxUpdateSucceed);
            }
        }
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getMaxAndReset() {
        /*
          update current max in case there was no calls involved before building report
         */
        call(0);
        return maxCallCountPerSecond.getAndSet(0);
    }

    public void reset() {
        getMaxAndReset();
    }
}

