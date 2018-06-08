package ru.fix.commons.profiler;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.impl.SimpleProfiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Kamil Asfandiyarov
 */
public class ProfilingTest {
    private static final Logger log = LoggerFactory.getLogger(ProfilingTest.class);

    private void doSomething(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


    @Test
    public void report_name() throws Exception {
        Profiler profiler = new SimpleProfiler();


        try (ProfilerReporter reporter = profiler.createReporter()) {

            ProfiledCall call = profiler.profiledCall("report_name");
            call.start();
            call.stop();

            ProfilerReport report = reporter.buildReportAndReset();
            assertEquals(1, report.getProfilerCallReports().size());
            assertEquals("report_name", report.getProfilerCallReports().get(0).getName());
        }
    }

    @Test
    public void single_thread_fixed_throughput() throws Exception {

        Profiler profiler = new SimpleProfiler();
        RateLimiter rateLimiter = RateLimiter.create(50);


        ProfiledCall call = profiler.profiledCall("single_thread_fixed_throughput");
        try (ProfilerReporter reporter = profiler.createReporter()) {


            reporter.buildReportAndReset();

            for (int i = 0; i < 100; i++) {
                call.start();
                rateLimiter.acquire();
                call.stop();
            }

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.callsThroughput, lessThan(60L));
        }
    }

    @Test
    public void single_thread_fixed_latency() throws Exception {

        Profiler profiler = new SimpleProfiler();


        ProfiledCall call = profiler.profiledCall("single_thread_fixed_latency");
        try (ProfilerReporter reporter = profiler.createReporter()) {

            reporter.buildReportAndReset();


            for (int i = 0; i < 50; i++) {
                call.start();
                doSomething(100);
                call.stop();
            }

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.minLatency, greaterThanOrEqualTo(90L));
        }
    }

    @Test
    public void parallel_threads_fixed_throughput() throws Exception {

        Profiler profiler = new SimpleProfiler();
        RateLimiter rateLimiter = RateLimiter.create(100);

        ExecutorService pool = Executors.newCachedThreadPool();

        try (ProfilerReporter reporter = profiler.createReporter()) {
            reporter.buildReportAndReset();

            for (int thread = 0; thread < 10; thread++) {
                pool.submit(() -> {
                    ProfiledCall call = profiler.profiledCall("parallel_threads_fixed_throughput");
                    for (int i = 0; i < 100; i++) {
                        call.start();
                        rateLimiter.acquire();
                        call.stop();
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(3, TimeUnit.MINUTES);

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.callsThroughput, lessThanOrEqualTo(110L));
        }
    }

    @Test
    public void parallel_threads_fixed_latency() throws Exception {

        Profiler profiler = new SimpleProfiler();
        try (ProfilerReporter reporter = profiler.createReporter()) {
            ExecutorService pool = Executors.newCachedThreadPool();

            reporter.buildReportAndReset();

            for (int thread = 0; thread < 10; thread++) {
                pool.submit(() -> {
                    ProfiledCall call = profiler.profiledCall("parallel_threads_fixed_latency");
                    for (int i = 0; i < 100; i++) {
                        call.start();
                        doSomething(50);
                        call.stop();
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(3, TimeUnit.MINUTES);

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.minLatency, greaterThanOrEqualTo(30L));
        }
    }

    @Test
    public void between_thread_call_fixed_latency() throws Exception {

        Profiler profiler = new SimpleProfiler();
        try (ProfilerReporter reporter = profiler.createReporter()) {
            ExecutorService pool1 = Executors.newCachedThreadPool();
            ExecutorService pool2 = Executors.newCachedThreadPool();

            reporter.buildReportAndReset();

            CompletableFuture[] futures = new CompletableFuture[20];

            for (int callNumber = 0; callNumber < futures.length; callNumber++) {

                ExecutorService poolA;
                ExecutorService poolB;

                if (callNumber % 2 == 0) {
                    poolA = pool1;
                    poolB = pool2;
                } else {
                    poolA = pool2;
                    poolB = pool1;
                }

                ProfiledCall call = profiler.profiledCall("between_thread_call_fixed_latency");

                CompletableFuture<Void> future = CompletableFuture
                        .runAsync(() -> {
                            call.start();
                            doSomething(getRandomInt(100, 200));
                        }, poolA)

                        .thenRunAsync(() -> doSomething(getRandomInt(100, 200)), poolB)

                        .thenRunAsync(() -> {
                            doSomething(getRandomInt(100, 200));
                            call.stop();
                        }, poolA);

                future.exceptionally(throwable -> {
                    log.error("Failed to build future chain", throwable);
                    return null;
                });

                futures[callNumber] = future;

            }
            log.info("before await");

            CompletableFuture.allOf(futures).join();

            log.info("after await");

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.minLatency, greaterThanOrEqualTo(250L));
        }
    }

    private static int getRandomInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }

    @Test
    public void simple_start_stop_with_zero_payload() throws Exception {
        Profiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        ProfiledCall call = profiler.profiledCall("simple_start_stop_with_zero_payload");


        reporter.buildReportAndReset();

        call.start();
        Thread.sleep(100);
        call.stop(0);

        ProfilerReport report = reporter.buildReportAndReset();

        log.info("Report: {}", report);

        assertEquals(0, report.getProfilerCallReports().get(0).payloadTotal);

    }

    @Test
    public void payload_min_max_total() throws Exception {

        Profiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        ProfiledCall call = profiler.profiledCall("payload_min_max_total");

        reporter.buildReportAndReset();

        call.start();
        call.stop();

        call.start();
        call.stop(12);

        call.start();
        call.stop(6);

        ProfilerReport report = reporter.buildReportAndReset();

        log.info("Report: {}", report);

        ProfilerCallReport callReport = report.getProfilerCallReports().get(0);
        assertEquals(1, callReport.payloadMin);
        assertEquals(12, callReport.payloadMax);
        assertEquals(1 + 12 + 6, callReport.payloadTotal);
    }

    @Test
    public void skip_empty_metrics() throws Exception {
        Profiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        ProfiledCall call = profiler.profiledCall("call_1");
        ProfiledCall call2 = profiler.profiledCall("call_2");

        call.start();
        call.stop();
        call.start();
        call.stop();

        call2.start();
        call2.stop();

        ProfilerReport report = reporter.buildReportAndReset();
        assertTrue(report.getIndicators().isEmpty());
        assertEquals(2, report.getProfilerCallReports().size());
        assertEquals(2L, report.getProfilerCallReports().get(0).getCallsCount());
        assertEquals("call_1", report.getProfilerCallReports().get(0).getName());
        assertEquals(1L, report.getProfilerCallReports().get(1).getCallsCount());
        assertEquals("call_2", report.getProfilerCallReports().get(1).getName());

        call2.start();
        call2.stop();
        call2.start();
        call2.stop();
        call2.start();
        call2.stop();

        report = reporter.buildReportAndReset();
        assertTrue(report.getIndicators().isEmpty());
        assertEquals(1, report.getProfilerCallReports().size());
        assertEquals(3L, report.getProfilerCallReports().get(0).getCallsCount());
        assertEquals("call_2", report.getProfilerCallReports().get(0).getName());
    }

    @Test
    public void reportBuildAndReset() throws Exception {
        Profiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        AtomicInteger threadIdx = new AtomicInteger();
        int writers = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        ExecutorService executorService = Executors.newFixedThreadPool(writers,
                r -> new Thread(r, "thread-" + threadIdx.getAndIncrement())
        );

        LongAdder callCount = new LongAdder();
        AtomicBoolean isRunning = new AtomicBoolean(true);
        for (int i = 0; i < writers; i++) {
            executorService.execute(() -> {
                while (isRunning.get()) {
                    ProfiledCall profiledCall = profiler.startProfiledCall(Thread.currentThread().getName());
                    profiledCall.stop();

                    callCount.increment();
                }
            });
        }


        List<ProfilerReport> reports = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            TimeUnit.MILLISECONDS.sleep(100);

            reports.add(reporter.buildReportAndReset());
        }

        isRunning.set(false);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        reports.add(reporter.buildReportAndReset());

        long callCountFromReports = reports.stream()
                .flatMap(profilerReport -> profilerReport.getProfilerCallReports().stream())
                .map(ProfilerCallReport::getCallsCount)
                .reduce(0L, Long::sum);

        assertEquals(callCount.sum(), callCountFromReports);
    }

}