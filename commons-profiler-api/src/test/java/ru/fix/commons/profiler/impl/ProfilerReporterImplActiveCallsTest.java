package ru.fix.commons.profiler.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;

import java.util.Collection;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Tim Urmancheev
 */
public class ProfilerReporterImplActiveCallsTest {

    private static final int numberOfActiveCallsToKeepBetweenReports = 20;
    private SimpleProfiler profiler;
    private ProfilerReporterImpl reporter;

    @BeforeEach
    public void setup() {
        profiler = new SimpleProfiler();
        reporter = new ProfilerReporterImpl(
                profiler,
                true,
                numberOfActiveCallsToKeepBetweenReports
        );
    }

    @AfterEach
    public void tearDown() {
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

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(0, report.getActiveCallsMaxLatency());
    }

    @Test
    public void allCallsEnded_reports0activeCallsMaxLatency() {
        profiler.start("Test").stop();
        profiler.start("Test").close();

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(0, report.getActiveCallsMaxLatency());
    }

    @Test
    public void hasActiveAndEndedCalls_usesCorrectCallForActiveCallsMaxLatency() throws InterruptedException {
        ProfiledCallImpl call1 = (ProfiledCallImpl) profiler.start("Test");
        Thread.sleep(100);
        ProfiledCallImpl call2 = (ProfiledCallImpl) profiler.start("Test");
        Thread.sleep(100);
        ProfiledCallImpl call3 = (ProfiledCallImpl) profiler.start("Test");

        call1.stop();

        long call1Time = call1.timeFromCallStartInMs();
        long call2Time = call2.timeFromCallStartInMs();
        long call3Time = call3.timeFromCallStartInMs();

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        long call2AfterReportTime = call2.timeFromCallStartInMs();

        assertTrue(call3Time < report.getActiveCallsMaxLatency() &&
                report.getActiveCallsMaxLatency() < call1Time
        );
        assertTrue(call2Time <= report.getActiveCallsMaxLatency() &&
                report.getActiveCallsMaxLatency() <= call2AfterReportTime
        );
    }

    @Test
    public void hasEndedCalls_resetsActiveCallsToLimit() {
        Collection<ProfiledCall> longestCalls = new LinkedList<>();
        for (int i = 0; i < numberOfActiveCallsToKeepBetweenReports; i++) {
            longestCalls.add(profiler.start("Test"));
        }
        profiler.start("Test");
        profiler.start("Test");
        profiler.start("Test").stop();

        reporter.buildReportAndReset();

        reporter.applyToSharedCounters("Test", counters -> {
            assertEquals(numberOfActiveCallsToKeepBetweenReports, counters.getActiveCalls().size());
            assertTrue(counters.getActiveCalls().containsAll(longestCalls));
        });
    }

    @Test
    public void noCallsEnded_resetsActiveCallsToLimit() {
        Collection<ProfiledCall> longestCalls = new LinkedList<>();
        for (int i = 0; i < numberOfActiveCallsToKeepBetweenReports; i++) {
            longestCalls.add(profiler.start("Test"));
        }
        profiler.start("Test");
        profiler.start("Test");

        reporter.buildReportAndReset();

        reporter.applyToSharedCounters("Test", counters -> {
            assertEquals(numberOfActiveCallsToKeepBetweenReports, counters.getActiveCalls().size());
            assertTrue(counters.getActiveCalls().containsAll(longestCalls));
        });
    }

    @Test
    public void disableActiveCallsMaxLatency_afterCallsWereStarted_removesCallsFromActiveOnNextReport() {
        profiler.start("Test");
        profiler.start("Test");

        reporter.setEnableActiveCallsMaxLatency(false);
        reporter.buildReportAndReset();

        reporter.applyToSharedCounters("Test", counters ->
                assertTrue(counters.getActiveCalls().isEmpty())
        );
    }

    private ProfilerCallReport getCallReport(ProfilerReport profilerReport) {
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        return profilerReport.getProfilerCallReports().get(0);
    }
}
