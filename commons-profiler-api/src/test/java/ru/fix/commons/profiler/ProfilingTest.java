package ru.fix.commons.profiler;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.impl.SimpleProfiler;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;


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
                    ProfiledCall profiledCall = profiler.start(Thread.currentThread().getName());
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

    @Test
    void profile_not_explicitly_stopped() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        profiler.call("call");
        // try-with-resources
        try (ProfiledCall call = profiler.start("profile")) {
            // some work
            // without call.stop() profiledCall will be dropped
        }

        ProfilerReport profilerReport = reporter.buildReportAndReset();

        assertNotNull(profilerReport);
        List<ProfilerCallReport> reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(1, reports.size());
        ProfilerCallReport report = reports.get(0);
        assertEquals("call", report.name);
        assertEquals(0L, report.activeCallsCount);
    }

    @Test
    void profile_explicitly_stopped() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        profiler.call("call");
        // try-with-resources
        try (ProfiledCall call = profiler.start("profile")) {
            // some work
            call.stop();
        }

        ProfilerReport profilerReport;
        List<ProfilerCallReport> reports;

        profilerReport = reporter.buildReportAndReset();

        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfilerCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCount());
        }

        profiler.call("call");
        // runnable (w/o result)
        profiler.profile("profile", () -> {
        });

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfilerCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCount());
        }

        profiler.call("call");
        // supplier (w result)
        String result = profiler.profile("profile", () -> "qwe");

        assertEquals("qwe", result);

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfilerCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCount());
        }
    }

    @Test
    void profile_unchecked_future() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        profiler.call("call");

        CompletableFuture<String> future = profiler.profileFuture(
                "profile",
                () -> CompletableFuture.completedFuture(resThrowableUnchecked())
        );

        String res = future.get(1, TimeUnit.SECONDS);
        assertEquals("unchecked", res);

        ProfilerReport profilerReport;
        List<ProfilerCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfilerCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCount());
        }
    }

    @Test
    void profile_checked_future() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        profiler.call("call");

        CompletableFuture<String> future = profiler.profileFuture(
                "profile",
                profiledCall -> CompletableFuture.completedFuture(resThrowableChecked())
        );

        String res = future.get(1, TimeUnit.SECONDS);
        assertEquals("checked", res);

        ProfilerReport profilerReport;
        List<ProfilerCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfilerCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCount());
        }
    }

    @Test
    void try_with_resource() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        // try-with-resources
        try (ProfiledCall call = profiler.start("profile.1")) {
            // some work
            resThrowableUnchecked();
            call.stop();
        } catch (Exception ignore) {
            fail("unexpected exception");
        }
        try (ProfiledCall call = profiler.start("profile.2")) {
            // some work
            resThrowableChecked();
            call.stop();
        } catch (Exception ignore) {
            fail("unexpected exception");
        }
        try (ProfiledCall call = profiler.start("profile.3")) {
            // some work
            resThrowsUnchecked();
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }
        try (ProfiledCall call = profiler.start("profile.4")) {
            // some work
            resThrowsChecked();
            fail("exception was excepted");
        } catch (InterruptedException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }
        try (ProfiledCall call = profiler.start("profile.5")) {
            // some work
            voidThrowableUnchecked();
            call.stop();
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }
        try (ProfiledCall call = profiler.start("profile.6")) {
            // some work
            voidThrowableChecked();
            call.stop();
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }
        try (ProfiledCall call = profiler.start("profile.7")) {
            // some work
            voidThrowsUnchecked();
            fail("exception was excepted");
        } catch (Exception ignore) {
        }
        try (ProfiledCall call = profiler.start("profile.8")) {
            // some work
            voidThrowsChecked();
            fail("exception was excepted");
        } catch (InterruptedException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }

        ProfilerReport profilerReport;
        List<ProfilerCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(4, reports.size());
        Set<String> names = reports.stream()
                .map(ProfilerCallReport::getName)
                .collect(Collectors.toSet());
        assertEquals(
                new HashSet<>(Arrays.asList("profile.1", "profile.2", "profile.5", "profile.6")),
                names
        );
    }

    @Test
    void blocks() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        profiler.profile("profile.1", ProfilingTest::resThrowableUnchecked);
        //profiler.profile("profile.2", ProfilingTest::resThrowableChecked); // not supported yet
        try {
            profiler.profile("profile.3", ProfilingTest::resThrowsUnchecked);
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }
//        profiler.profile("profile.4", ProfilingTest::resThrowsChecked); // not supported yet
        profiler.profile("profile.5", ProfilingTest::voidThrowableUnchecked);
//        profiler.profile("profile.6", ProfilingTest::voidThrowableChecked); // not supported yet
        try {
            profiler.profile("profile.7", ProfilingTest::voidThrowsUnchecked);
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }
//        profiler.profile("profile.8", ProfilingTest::voidThrowsChecked); // not supported yet

        ProfilerReport profilerReport;
        List<ProfilerCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        Set<String> names = reports.stream()
                .map(ProfilerCallReport::getName)
                .collect(Collectors.toSet());
        assertEquals(
                new HashSet<>(Arrays.asList("profile.1", "profile.5")),
                names
        );
    }

    @Test
    void profile_futures() throws Exception {
        SimpleProfiler profiler = new SimpleProfiler();
        ProfilerReporter reporter = profiler.createReporter(true, 10);

        CompletableFuture<String> future;
        String s;

        future = profiler.profileFuture(
                "profile.1",
                () -> cfSupplierThrowableUncheckedSuccess()
        );
        s = future.get();
        assertEquals("unchecked", s);

        future = profiler.profileFuture(
                "profile.2",
                () -> cfSupplierThrowableUncheckedExc()
        );
        try {
            future.get();
            fail("exception was excepted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NumberFormatException) {
                // expected exception
            } else {
                fail("unexpected exception");
            }
        } catch (Exception e) {
            fail("unexpected exception");
        }

        try {
            future = profiler.profileFuture(
                    "profile.3",
                    () -> cfSupplierThrowsUnchecked()
            );
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }

        try {
            future = profiler.profileFuture(
                    "profile.4",
                    profiledCall -> cfSupplierThrowsUnchecked()
            );
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }

        try {
            future = profiler.profileFuture(
                    "profile.5",
                    profilerCall -> cfSupplierThrowableCheckedSuccess()
            );
        } catch (InterruptedException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }
        s = future.get();
        assertEquals("checked", s);

        try {
            future = profiler.profileFuture(
                    "profile.6",
                    profilerCall -> cfSupplierThrowableCheckedExc()
            );
        } catch (Exception e) {
            fail("unexpected exception");
        }
        try {
            future.get();
            fail("exception was excepted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NumberFormatException) {
                // expected exception
            } else {
                fail("unexpected exception");
            }
        } catch (Exception e) {
            fail("unexpected exception");
        }

        try {
            future = profiler.profileFuture(
                    "profile.7",
                    profilerCall -> cfSupplierThrowsChecked()
            );
        } catch (InterruptedException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }

        ProfilerReport profilerReport;
        List<ProfilerCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        Set<String> names = reports.stream()
                .map(ProfilerCallReport::getName)
                .collect(Collectors.toSet());
        assertEquals(
                new HashSet<>(Arrays.asList("profile.1", "profile.5")),
                names
        );
    }

    private static String resThrowableUnchecked() throws IllegalArgumentException {
        return "unchecked";
    }

    private static String resThrowableChecked() throws InterruptedException {
        return "checked";
    }

    private static String resThrowsUnchecked() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    private static String resThrowsChecked() throws InterruptedException {
        throw new InterruptedException();
    }

    private static void voidThrowableUnchecked() throws IllegalArgumentException {
    }

    private static void voidThrowableChecked() throws InterruptedException {
    }

    private static void voidThrowsUnchecked() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    private static void voidThrowsChecked() throws InterruptedException {
        throw new InterruptedException();
    }

    private static CompletableFuture<String> cfSupplierThrowableUncheckedSuccess() throws IllegalArgumentException {
        return CompletableFuture.completedFuture("unchecked");
    }

    private static CompletableFuture<String> cfSupplierThrowableUncheckedExc() throws IllegalArgumentException {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new NumberFormatException());
        return future;
    }

    private static CompletableFuture<String> cfSupplierThrowsUnchecked() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    private static CompletableFuture<String> cfSupplierThrowableCheckedSuccess() throws InterruptedException {
        return CompletableFuture.completedFuture("checked");
    }

    private static CompletableFuture<String> cfSupplierThrowableCheckedExc() throws InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new NumberFormatException());
        return future;
    }

    private static CompletableFuture<String> cfSupplierThrowsChecked() throws InterruptedException {
        throw new InterruptedException();
    }
}