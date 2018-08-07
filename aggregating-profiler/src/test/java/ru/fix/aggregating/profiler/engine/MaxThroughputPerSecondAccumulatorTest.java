package ru.fix.aggregating.profiler.engine;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaxThroughputPerSecondAccumulatorTest {
    private static final Logger log = LoggerFactory.getLogger(MaxThroughputPerSecondAccumulatorTest.class);

    @Test
    public void testCall() throws Exception {
        MaxThroughputPerSecondAccumulator accumulator = new MaxThroughputPerSecondAccumulator();
        long timestamp = System.currentTimeMillis();

        accumulator.call(timestamp, 1);
        accumulator.call(timestamp, 3);
        assertEquals(4, accumulator.getAndReset(timestamp + 1000));

        accumulator.call(timestamp + 1000, 5);
        accumulator.call(timestamp + 2000, 13);
        accumulator.call(timestamp + 3000, 3);

        assertEquals(13, accumulator.getAndReset (timestamp + 3000));
    }

    @Test
    public void generate_fixed_rate_events_and_check_max_throughput() throws Exception {
        final int COUNT_OF_THREADS = 5;
        final int BUILD_REPORT_ITERATION_COUNT = 3;

        // One thread events per second rate = 1000 / ACCURACY
        // One thread will generate ~ 100 events in a second for ACCURACY = 10
        final int ACCURACY = 10;

        final int EXPECTED_MAX_THROUGHPUT = COUNT_OF_THREADS * 1000 / ACCURACY;

        MaxThroughputPerSecondAccumulator calculator = new MaxThroughputPerSecondAccumulator();
        ExecutorService pool = Executors.newFixedThreadPool(COUNT_OF_THREADS);

        AtomicBoolean shutdownFlag = new AtomicBoolean();

        for (int i = 0; i < COUNT_OF_THREADS; i++) {
            pool.submit(() -> {

                long startTime = System.currentTimeMillis();
                long startTicks = System.currentTimeMillis() / ACCURACY;

                // Test will run no more than 30 seconds until shutdownFlag is set
                while (System.currentTimeMillis() < startTime + TimeUnit.SECONDS.toMillis(30) && !shutdownFlag.get()) {
                    long currentTicks = System.currentTimeMillis() / ACCURACY;

                    long ticksDiff = currentTicks - startTicks;

                    if (ticksDiff > 0) {
                        calculator.call(System.currentTimeMillis(), ticksDiff);
                    } else {
                        Thread.yield();
                    }

                    startTicks = currentTicks;
                }
            });
        }


        // Build and print report BUILD_REPORT_ITERATION_COUNT times.
        // Check last report result

        AtomicInteger buildReportAttempt = new AtomicInteger();
        AtomicReference<ScheduledFuture> reportBuildingSchedule = new AtomicReference<>();
        AtomicLong lastReportResult = new AtomicLong();

        reportBuildingSchedule.set(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> {
                    long result = calculator.getAndReset(System.currentTimeMillis());
                    log.info("MaxThroughput: {}", result);
                    int reportBuildAttemptNumber = buildReportAttempt.incrementAndGet();

                    if (reportBuildAttemptNumber == BUILD_REPORT_ITERATION_COUNT) {
                        lastReportResult.set(result);
                        reportBuildingSchedule.get().cancel(false);
                        shutdownFlag.set(true);
                    }
                },
                0,
                5,
                TimeUnit.SECONDS));

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);


        log.info("Expected max throughput: {}, Actual: {}", EXPECTED_MAX_THROUGHPUT, lastReportResult.get());
        assertTrue(lastReportResult.get() > EXPECTED_MAX_THROUGHPUT * 0.9);
        assertTrue(lastReportResult.get() < EXPECTED_MAX_THROUGHPUT * 1.1);

    }

}