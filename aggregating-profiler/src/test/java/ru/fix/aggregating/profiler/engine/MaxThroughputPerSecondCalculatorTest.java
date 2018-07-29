package ru.fix.aggregating.profiler.engine;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class MaxThroughputPerSecondCalculatorTest {
    private static final Logger log = LoggerFactory.getLogger(MaxThroughputPerSecondCalculatorTest.class);

    @Test
    public void testCall() throws Exception {
        MaxThroughputPerSecondCalculator c = Mockito.spy(new MaxThroughputPerSecondCalculator());

        when(c.currentTimeMillis()).thenReturn(1000L, 1100L, 2000L);
        c.call();
        c.call();
        c.call();
        assertEquals(2, c.getMaxAndReset());

        when(c.currentTimeMillis()).thenReturn(3000L, 4000L, 4100L, 4200L, 5000L);
        c.call();
        c.call();
        c.call();
        c.call();
        assertEquals(3, c.getMaxAndReset());

        assertEquals(0, c.getMaxAndReset());
    }

    @Test
    public void generate_fixed_rate_events_and_check_max_throughput() throws Exception {
        final int COUNT_OF_THREADS = 5;
        final int BUILD_REPORT_ITERATION_COUNT = 3;

        // One thread events per second rate = 1000 / ACCURACY
        // One thread will generate ~ 100 events in a second for ACCURACY = 10
        final int ACCURACY = 10;

        final int EXPECTED_MAX_THROUGHPUT = COUNT_OF_THREADS * 1000 / ACCURACY;

        MaxThroughputPerSecondCalculator calculator = new MaxThroughputPerSecondCalculator();
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
                        calculator.call(ticksDiff);
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
                    long result = calculator.getMaxAndReset();
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