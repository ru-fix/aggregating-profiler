package ru.fix.aggregating.profiler;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AggregatingProfilerTest {
    private static final Logger log = LoggerFactory.getLogger(AggregatingProfilerTest.class);

    private void sleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exc) {
            log.error(exc.getMessage(), exc);
        }
    }


    @Test
    void report_name() throws Exception {
        Profiler profiler = new AggregatingProfiler();

        try (ProfilerReporter reporter = profiler.createReporter()) {

            ProfiledCall call = profiler.profiledCall("call.name");
            call.start();
            call.stop();

            ProfilerReport report = reporter.buildReportAndReset();
            assertEquals(1, report.getProfilerCallReports().size());
            assertEquals("call.name", report.getProfilerCallReports().get(0).getName());
        }
    }

    @Test
    void single_thread_fixed_throughput() throws Exception {

        Profiler profiler = new AggregatingProfiler();

        ProfiledCall call = profiler.profiledCall("single_thread_fixed_throughput");
        try (ProfilerReporter reporter = profiler.createReporter()) {

            AtomicInteger counter = new AtomicInteger();

            new FixedRateEventEmitter(50, () -> {
                call.start();
                call.stop();
                return counter.incrementAndGet() < 100;
            }).join();

            ProfiledCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.callsThroughputAvg, lessThanOrEqualTo(70.0));
            assertThat(report.callsThroughputAvg, greaterThanOrEqualTo(40.0));
        }
    }

    @Test
    void single_thread_fixed_latency() throws Exception {

        Profiler profiler = new AggregatingProfiler();

        ProfiledCall call = profiler.profiledCall("single_thread_fixed_latency");
        try (ProfilerReporter reporter = profiler.createReporter()) {

            reporter.buildReportAndReset();


            for (int i = 0; i < 50; i++) {
                call.start();
                sleep(100);
                call.stop();
            }

            ProfiledCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.latencyMin, greaterThanOrEqualTo(90L));
            assertThat(report.latencyMin, lessThanOrEqualTo(120L));
        }
    }

    @Test
    void single_thread_fixed_latency_start_nanotime() throws Exception {
        Profiler profiler = new AggregatingProfiler();

        ProfiledCall call = profiler.profiledCall("single_thread_fixed_latency");
        try (ProfilerReporter reporter = profiler.createReporter()) {

            reporter.buildReportAndReset();


            for (int i = 0; i < 50; i++) {
                long startTime = System.currentTimeMillis();
                sleep(100);
                call.call(startTime, i);
            }

            ProfiledCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.latencyMin, greaterThanOrEqualTo(90L));
            assertThat(report.latencyMin, lessThanOrEqualTo(120L));
            assertThat(report.payloadMax, equalTo(49L));
            assertThat(report.payloadMin, equalTo(0L));
        }
    }


    @Test
    void parallel_threads_fixed_latency() throws Exception {

        Profiler profiler = new AggregatingProfiler();
        try (ProfilerReporter reporter = profiler.createReporter()) {
            ExecutorService pool = Executors.newCachedThreadPool();

            reporter.buildReportAndReset();

            for (int thread = 0; thread < 10; thread++) {
                pool.submit(() -> {
                    ProfiledCall call = profiler.profiledCall("parallel_threads_fixed_latency");
                    for (int i = 0; i < 100; i++) {
                        call.start();
                        sleep(50);
                        call.stop();
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(3, TimeUnit.MINUTES);

            ProfiledCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.latencyMin, greaterThanOrEqualTo(30L));
            assertThat(report.latencyMin, lessThanOrEqualTo(70L));
        }
    }

    @Test
    void between_thread_call_fixed_latency() throws Exception {

        Profiler profiler = new AggregatingProfiler();
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
                            sleep(getRandomInt(100, 200));
                        }, poolA)

                        .thenRunAsync(() -> sleep(getRandomInt(100, 200)), poolB)

                        .thenRunAsync(() -> {
                            sleep(getRandomInt(100, 200));
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

            ProfiledCallReport report = reporter.buildReportAndReset().getProfilerCallReports().get(0);
            log.info(report.toString());

            assertThat(report.latencyMin, greaterThanOrEqualTo(250L));
        }
    }

    private static int getRandomInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }

    @Test
    void simple_start_stop_with_zero_payload() throws Exception {
        Profiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        ProfiledCall call = profiler.profiledCall("simple_start_stop_with_zero_payload");


        reporter.buildReportAndReset();

        call.start();
        Thread.sleep(100);
        call.stop(0);

        ProfilerReport report = reporter.buildReportAndReset();

        log.info("Report: {}", report);

        assertEquals(0, report.getProfilerCallReports().get(0).payloadSum);

    }

    @Test
    void payload_min_max_total() throws Exception {

        Profiler profiler = new AggregatingProfiler();
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

        ProfiledCallReport callReport = report.getProfilerCallReports().get(0);
        assertEquals(1, callReport.payloadMin);
        assertEquals(12, callReport.payloadMax);
        assertEquals(1 + 12 + 6, callReport.payloadSum);
    }

    @Test
    void skip_empty_metrics() throws Exception {
        Profiler profiler = new AggregatingProfiler();
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
        assertEquals(2L, report.getProfilerCallReports().get(0).getCallsCountSum());
        assertEquals("call_1", report.getProfilerCallReports().get(0).getName());
        assertEquals(1L, report.getProfilerCallReports().get(1).getCallsCountSum());
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
        assertEquals(3L, report.getProfilerCallReports().get(0).getCallsCountSum());
        assertEquals("call_2", report.getProfilerCallReports().get(0).getName());
    }

    @Test
    void reportBuildAndReset() throws Exception {
        final int WRITERS = 5;

        Profiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        AtomicInteger threadIdx = new AtomicInteger();

        ExecutorService executorService = Executors.newFixedThreadPool(WRITERS,
                r -> new Thread(r, "thread-" + threadIdx.getAndIncrement())
        );

        LongAdder callCount = new LongAdder();
        AtomicBoolean isRunning = new AtomicBoolean(true);
        for (int i = 0; i < WRITERS; i++) {
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
        assertTrue( executorService.awaitTermination(5, TimeUnit.SECONDS) );

        reports.add(reporter.buildReportAndReset());

        long callCountFromReports = reports.stream()
                .flatMap(profilerReport -> profilerReport.getProfilerCallReports().stream())
                .map(ProfiledCallReport::getCallsCountSum)
                .reduce(0L, Long::sum);



        assertEquals(callCount.sum(), callCountFromReports, ()-> "Current report: " + reporter.buildReportAndReset());
    }

    @Test
    void profile_not_explicitly_stopped() {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        profiler.call("call");
        // try-with-resources
        try (ProfiledCall call = profiler.start("profile")) {
            // some work
            // without call.stop() profiledCall will be dropped
        }

        ProfilerReport profilerReport = reporter.buildReportAndReset();

        assertNotNull(profilerReport);
        List<ProfiledCallReport> reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(1, reports.size());
        ProfiledCallReport report = reports.get(0);
        assertEquals("call", report.name);
        assertEquals(0L, report.activeCallsCountMax);
    }

    @Test
    void profile_explicitly_stopped() throws Exception {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        profiler.call("call");
        // try-with-resources
        try (ProfiledCall call = profiler.start("profile")) {
            // some work
            call.stop();
        }

        ProfilerReport profilerReport;
        List<ProfiledCallReport> reports;

        profilerReport = reporter.buildReportAndReset();

        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfiledCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCountMax());
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
        for (ProfiledCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCountMax());
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
        for (ProfiledCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCountMax());
        }
    }

    @Test
    void profile_unchecked_future() throws Exception {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        profiler.call("call");

        CompletableFuture<String> future = profiler.profileFuture(
                "profile",
                () -> CompletableFuture.completedFuture(resThrowableUnchecked())
        );

        String res = future.get(1, TimeUnit.SECONDS);
        assertEquals("unchecked", res);

        ProfilerReport profilerReport;
        List<ProfiledCallReport> reports;

        profilerReport = reporter.buildReportAndReset();

        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfiledCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCountMax());
        }
    }

    @Test
    void profile_checked_future() throws Exception {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        profiler.call("call");

        CompletableFuture<String> future = profiler.profileFutureThrowable(
                "profile",
                () -> CompletableFuture.completedFuture(resThrowableChecked())
        );

        String res = future.get(1, TimeUnit.SECONDS);
        assertEquals("checked", res);

        ProfilerReport profilerReport;
        List<ProfiledCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        for (ProfiledCallReport report : reports) {
            assertEquals(0L, report.getActiveCallsCountMax());
        }
    }

    @Test
    void try_with_resource() throws Exception {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

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
        List<ProfiledCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(4, reports.size());
        Set<String> names = reports.stream()
                .map(ProfiledCallReport::getName)
                .collect(Collectors.toSet());
        assertEquals(
                new HashSet<>(Arrays.asList("profile.1", "profile.2", "profile.5", "profile.6")),
                names
        );
    }

    @Test
    void blocks() throws Exception {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

        profiler.profile("profile.1", AggregatingProfilerTest::resThrowableUnchecked);
        //profiler.profile("profile.2", ProfilingTest::resThrowableChecked); // not supported yet
        try {
            profiler.profile("profile.3", AggregatingProfilerTest::resThrowsUnchecked);
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }
//        profiler.profile("profile.4", ProfilingTest::resThrowsChecked); // not supported yet
        profiler.profile("profile.5", AggregatingProfilerTest::voidThrowableUnchecked);
//        profiler.profile("profile.6", ProfilingTest::voidThrowableChecked); // not supported yet
        try {
            profiler.profile("profile.7", AggregatingProfilerTest::voidThrowsUnchecked);
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }
//        profiler.profile("profile.8", ProfilingTest::voidThrowsChecked); // not supported yet

        ProfilerReport profilerReport;
        List<ProfiledCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        Set<String> names = reports.stream()
                .map(ProfiledCallReport::getName)
                .collect(Collectors.toSet());
        assertEquals(
                new HashSet<>(Arrays.asList("profile.1", "profile.5")),
                names
        );
    }

    @Test
    void profile_futures() throws Exception {
        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfilerReporter reporter = profiler.createReporter();

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
            future = profiler.profileFutureThrowable(
                    "profile.4",
                    () -> cfSupplierThrowsUnchecked()
            );
            fail("exception was excepted");
        } catch (IllegalArgumentException ignore) {
            // expected exception
        } catch (Exception ignore) {
            fail("unexpected exception");
        }

        try {
            future = profiler.profileFutureThrowable(
                    "profile.5",
                    () -> cfSupplierThrowableCheckedSuccess()
            );
        } catch (InterruptedException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }
        s = future.get();
        assertEquals("checked", s);

        try {
            future = profiler.profileFutureThrowable(
                    "profile.6",
                    () -> cfSupplierThrowableCheckedExc()
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
            future = profiler.profileFutureThrowable(
                    "profile.7",
                    () -> cfSupplierThrowsChecked()
            );
        } catch (InterruptedException ignore) {
            // expected exception
        } catch (Exception e) {
            fail("unexpected exception");
        }

        ProfilerReport profilerReport;
        List<ProfiledCallReport> reports;

        profilerReport = reporter.buildReportAndReset();
        assertNotNull(profilerReport);
        reports = profilerReport.getProfilerCallReports();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        Set<String> names = reports.stream()
                .map(ProfiledCallReport::getName)
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


    @Test
    public void indicatorNameEndsWithIndicatorMaxSuffix(){
        Profiler profiler = new AggregatingProfiler();
        profiler.attachIndicator("my.indicator", () -> 147L);
        ProfilerReporter reporter = profiler.createReporter();
        Map<String, Long> indicators = reporter.buildReportAndReset().getIndicators();
        assertTrue(indicators.containsKey("my.indicator.indicatorMax"));
        assertEquals(147L, indicators.get("my.indicator.indicatorMax").longValue());
    }

    @Test
    public void indicatorNullReturnValue(){
        Profiler profiler = new AggregatingProfiler();
        profiler.attachIndicator("my.indicator", () -> null);
        ProfilerReporter reporter = profiler.createReporter();
        Map<String, Long> indicators = reporter.buildReportAndReset().getIndicators();
        assertFalse(indicators.containsKey("my.indicator.indicatorMax"));
    }
}
