package ru.fix.commons.profiler.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 11.01.18.
 */
public class CalculateMaxThroughput {
    private final AtomicLong maxCallCountPerSecond = new AtomicLong();
    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong timeBeginningOfSecond = new AtomicLong();
    private static final long ONE_SECOND_MS = 1_000;


    public void call() {
        call(1);
    }

    public void call(long eventCount) {
        long time = timeBeginningOfSecond.get();
        long count = callCount.addAndGet(eventCount);
        long max = maxCallCountPerSecond.get();
        long now = currentTimeMillis();

        if (time + ONE_SECOND_MS < now) {
            if (timeBeginningOfSecond.compareAndSet(time, now)) {
                callCount.addAndGet(-count);
                if (count > max) {
                    maxCallCountPerSecond.compareAndSet(max, count);
                }
            }
        }
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getMaxAndReset() {
        call(0);
        timeBeginningOfSecond.set(currentTimeMillis());
        return maxCallCountPerSecond.getAndSet(0);
    }

    public void reset() {
        getMaxAndReset();
    }
}

