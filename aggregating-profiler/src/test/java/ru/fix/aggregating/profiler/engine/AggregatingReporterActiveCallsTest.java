package ru.fix.aggregating.profiler.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.aggregating.profiler.AggregatingProfiler;
import ru.fix.aggregating.profiler.ProfiledCall;
import ru.fix.aggregating.profiler.ProfiledCallReport;
import ru.fix.aggregating.profiler.ProfilerReport;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Tim Urmancheev
 */
public class AggregatingReporterActiveCallsTest {

    private AggregatingProfiler profiler;
    private AggregatingReporter reporter;

    private final int numberOfActiveCallsToTrackAndKeepBetweenReports = 25;

    @BeforeEach
    public void setup() {
        profiler = new AggregatingProfiler()
                        .setNumberOfActiveCallsToTrackAndKeepBetweenReports(25);
        reporter = (AggregatingReporter) profiler.createReporter();
    }


    @AfterEach
    public void tearDown() throws Exception {
        reporter.close();
    }

    @Test
    public void noCallsStartedOrCalled_reportBuildSuccess() {
        reporter.buildReportAndReset();
    }

    @Test
    public void noCallsStarted_reports0activeCallsMaxLatency() {
        profiler.profiledCall("Test").call();
        profiler.profiledCall("Test").call();

        ProfiledCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(0, report.getActiveCallsLatencyMax());
    }

    @Test
    public void allCallsEnded_reports0activeCallsMaxLatency() {
        profiler.start("Test").stop();
        profiler.start("Test").close();

        ProfiledCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(0, report.getActiveCallsLatencyMax());
    }

    @Test
    public void hasActiveAndEndedCalls_usesCorrectCallForActiveCallsMaxLatency() throws InterruptedException {
        AggregatingCall call1 = (AggregatingCall) profiler.start("Test");
        Thread.sleep(100);
        AggregatingCall call2 = (AggregatingCall) profiler.start("Test");
        Thread.sleep(100);
        AggregatingCall call3 = (AggregatingCall) profiler.start("Test");

        call1.stop();

        long call1Time = call1.timeFromCallStart();
        long call2Time = call2.timeFromCallStart();
        long call3Time = call3.timeFromCallStart();

        ProfiledCallReport report = getCallReport(reporter.buildReportAndReset());

        long call2AfterReportTime = call2.timeFromCallStart();

        assertTrue(call3Time < report.getActiveCallsLatencyMax() &&
                report.getActiveCallsLatencyMax() < call1Time
        );
        assertTrue(call2Time <= report.getActiveCallsLatencyMax() &&
                report.getActiveCallsLatencyMax() <= call2AfterReportTime
        );
    }

    @Test
    public void hasEndedCalls_resetsActiveCallsToLimit() throws Exception {
        Collection<ProfiledCall> longestCalls = new ArrayList<>();
        for (int i = 0; i < numberOfActiveCallsToTrackAndKeepBetweenReports; i++) {
            longestCalls.add(profiler.start("Test"));
        }

        Thread.sleep(100);

        profiler.start("Test");
        profiler.start("Test");
        profiler.start("Test").stop();

        reporter.buildReportAndReset();

        reporter.updateCounters("Test", counters -> {
            assertEquals(numberOfActiveCallsToTrackAndKeepBetweenReports, counters.getActiveCalls().size());
            assertTrue(counters.getActiveCalls().containsAll(longestCalls));
        });
    }

    @Test
    public void noCallsEnded_resetsActiveCallsToLimit() throws Exception{
        Collection<ProfiledCall> longestCalls = new ArrayList<>();
        for (int i = 0; i < numberOfActiveCallsToTrackAndKeepBetweenReports; i++) {
            longestCalls.add(profiler.start("Test"));
        }

        Thread.sleep(100);

        profiler.start("Test");
        profiler.start("Test");

        reporter.buildReportAndReset();

        reporter.updateCounters("Test", counters -> {
            assertEquals(numberOfActiveCallsToTrackAndKeepBetweenReports, counters.getActiveCalls().size());
            assertTrue(counters.getActiveCalls().containsAll(longestCalls));
        });
    }

    @Test
    public void disableActiveCallsMaxLatency_afterCallsWereStarted_removesCallsFromActiveOnNextReport() {
        profiler.start("Test");
        profiler.start("Test");

        profiler.setNumberOfActiveCallsToTrackAndKeepBetweenReports(0);

        reporter.buildReportAndReset();


        reporter.updateCounters("Test", counters ->
                assertTrue(counters.getActiveCalls().isEmpty())
        );
    }

    private ProfiledCallReport getCallReport(ProfilerReport profilerReport) {
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        return profilerReport.getProfilerCallReports().get(0);
    }
}
