package ru.fix.commons.profiler.util;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 11.01.18.
 */
public class MaxThroughputCalculatorTest {
    private static final Logger log = LoggerFactory.getLogger(MaxThroughputCalculatorTest.class);


    @Test
    public void testCall() throws Exception {
        MaxThroughputCalculator c = spy(new MaxThroughputCalculator());

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
        MaxThroughputCalculator maxThroughput = new MaxThroughputCalculator();

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
        assertTrue("Throughput is not correct. " + mess,
                maxThrp > (perInSec - approximately) && maxThrp < (perInSec + approximately));
    }
}