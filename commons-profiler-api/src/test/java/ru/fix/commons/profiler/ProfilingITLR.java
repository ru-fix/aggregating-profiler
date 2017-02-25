package ru.fix.commons.profiler;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.impl.SimpleProfiler;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Kamil Asfandiyarov
 */
public class ProfilingITLR {
    private static final Logger log = LoggerFactory.getLogger(ProfilingITLR.class);

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

            assertTrue(report.callsThroughput < 60);
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

            assertEquals(true, report.minLatency >= 90);
        }
    }

    @Ignore//CPAPSM-3324
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

            assertEquals(true, report.callsThroughput <= 110);
        }
    }

    @Ignore//CPAPSM-3324
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
                        doSomething(100);
                        call.stop();
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(3, TimeUnit.MINUTES);

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertEquals(true, report.minLatency >= 90);
        }
    }

    @Ignore//CPAPSM-3324
    @Test
    public void between_thread_call_fixed_latency() throws Exception {

        Profiler profiler = new SimpleProfiler();
        try (ProfilerReporter reporter = profiler.createReporter()) {
            ExecutorService pool1 = Executors.newCachedThreadPool();
            ExecutorService pool2 = Executors.newCachedThreadPool();

            reporter.buildReportAndReset();

            CompletableFuture[] futures = new CompletableFuture[20_000];

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

                futures[callNumber] = CompletableFuture
                        .runAsync(() -> {
                            call.start();
                            doSomething(ThreadLocalRandom.current().nextInt(100, 200));
                        }, poolA)

                        .thenRunAsync(() -> doSomething(ThreadLocalRandom.current().nextInt(100, 200)), poolB)

                        .thenRunAsync(() -> {
                            doSomething(ThreadLocalRandom.current().nextInt(100, 200));
                            call.stop();
                        }, poolA);

            }
            log.info("before await");

            CompletableFuture.allOf(futures).join();

            log.info("after await");

            ProfilerCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertEquals(true, report.minLatency >= 250);
        }
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

        assertEquals("payloadTotal", 0, report.getProfilerCallReports().get(0).payloadTotal);

    }

    @Test
    public void payload_min_max_total() throws Exception{

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
        assertEquals("payloadMin", 1, callReport.payloadMin);
        assertEquals("payloadMax", 12, callReport.payloadMax);
        assertEquals("payloadMax", 1+12+6, callReport.payloadTotal);
    }

}