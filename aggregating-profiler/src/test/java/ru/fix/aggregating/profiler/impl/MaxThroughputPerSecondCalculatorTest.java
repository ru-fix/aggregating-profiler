package ru.fix.aggregating.profiler.impl;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.engine.MaxThroughputPerSecondCalculator;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 11.01.18.
 */
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
    public void loadTest() throws Exception {
        int threadCount = 3;
        int rateLimitInThread = 300;
        int testSecond = 5;

        int perInSec = threadCount * rateLimitInThread;
        AtomicLong allCount = new AtomicLong(perInSec * testSecond);

        RateLimiter[] rateLimiters = new RateLimiter[threadCount];
        for (int i = 0; i < threadCount; i++) {
            rateLimiters[i] = RateLimiter.create(rateLimitInThread);
        }

        CompletableFuture[] threads = new CompletableFuture[threadCount];
        AtomicLong callCount = new AtomicLong();
        MaxThroughputPerSecondCalculator maxThroughput = new MaxThroughputPerSecondCalculator();

        ExecutorService poolExecutor = Executors.newFixedThreadPool(threadCount);


        long startTime = System.currentTimeMillis();
        for (int threadNumber = 0; threadNumber < threadCount; threadNumber++) {
            final RateLimiter limiter = rateLimiters[threadNumber];
            threads[threadNumber] = CompletableFuture.runAsync(() -> {
                while (callCount.incrementAndGet() < allCount.get()) {
                    limiter.acquire();
                    maxThroughput.call();
                }
            }, poolExecutor);
        }
        CompletableFuture.allOf(threads).join();
        long stopTime = System.currentTimeMillis();


        long workSec = TimeUnit.MILLISECONDS.toSeconds(stopTime - startTime);
        long maxThrp = maxThroughput.getMaxAndReset();
        String mess = String.format("Input value: threadCount %s, rateLimitInThread %s, testSecond %s, perInSec %s. " +
                        "Test result: workSec %s, maxThrp '%s'.",
                threadCount, rateLimitInThread, testSecond, perInSec, workSec, maxThrp);
        log.info(mess);

        int approximately = (int) (perInSec * 0.10);
        assertTrue(
                maxThrp > (perInSec - approximately) && maxThrp < (perInSec + approximately),
                "Throughput is not correct. " + mess
        );
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