package ru.fix.commons.profiler.impl;

import org.junit.Test;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 26.12.17.
 */
public class ProfilerReporterImplTest {

    @Test
    public void callStopWithoutParams() throws Exception {
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        ProfilerReporterImpl reporter = new ProfilerReporterImpl(simpleProfiler);

        ProfiledCall call = simpleProfiler.profiledCall("Test");
        call.start();
        //someMethod()
        call.stop();

        ProfilerReport profilerReport = reporter.buildReportAndReset();
        assertFalse(profilerReport.getProfilerCallReports().isEmpty());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        ProfilerCallReport report = profilerReport.getProfilerCallReports().get(0);
        assertEquals(1, report.getCallsCount());
        assertEquals(1, report.getPayloadTotal());
        assertTrue("report time is not correct: " + report, report.getReportingTime() < 1000);

        reporter.close();
    }

    @Test
    public void callStopWithParams() throws Exception {
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        ProfilerReporterImpl reporter = new ProfilerReporterImpl(simpleProfiler);

        ProfiledCall call = simpleProfiler.profiledCall("Test");
        call.start();
        //someMethod()
        call.stop(30);

        ProfilerReport profilerReport = reporter.buildReportAndReset();
        assertFalse(profilerReport.getProfilerCallReports().isEmpty());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        assertEquals(1, profilerReport.getProfilerCallReports().get(0).getCallsCount());
        assertEquals(30, profilerReport.getProfilerCallReports().get(0).getPayloadTotal());

        reporter.close();
    }
}