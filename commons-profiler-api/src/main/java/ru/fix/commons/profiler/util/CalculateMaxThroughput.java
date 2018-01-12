package ru.fix.commons.profiler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 11.01.18.
 */
public class CalculateMaxThroughput {
    private static final Logger log = LoggerFactory.getLogger(CalculateMaxThroughput.class);

    private final AtomicLong maxCallCountPerSecond = new AtomicLong();
    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong timeBeginningOfSecond = new AtomicLong();
    private static final long ONE_SECOND_MS = 1_000;


    public void call() {
        call(1);
    }

    public void call(long eventCount) {
        long time = timeBeginningOfSecond.get();
        long count = callCount.getAndAdd(eventCount);
        long now = currentTimeMillis();

        log.trace("begin time {}, count {}, now {}", time, count, now);

        if (time + ONE_SECOND_MS <= now) {
            if (timeBeginningOfSecond.compareAndSet(time, now)) {
                callCount.addAndGet(-count);

                boolean doWhile;
                do {
                    long max = maxCallCountPerSecond.get();
                    log.trace("update max, count {}, max {}", count, max);
                    doWhile = count > max && !maxCallCountPerSecond.compareAndSet(max, count);
                } while (doWhile);
            }
        }
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getMaxAndReset() {
        /*
          update current max in case there was no calls involved before building report
         */
        log.trace("reset");
        call(0);
        return maxCallCountPerSecond.getAndSet(0);
    }

    public void reset() {
        getMaxAndReset();
    }
}

