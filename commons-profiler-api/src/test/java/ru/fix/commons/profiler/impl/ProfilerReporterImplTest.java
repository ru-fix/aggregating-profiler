package ru.fix.commons.profiler.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 26.12.17.
 */
public class ProfilerReporterImplTest {

    private SimpleProfiler profiler;
    private ProfilerReporterImpl reporter;

    @Before
    public void setup() {
        profiler = new SimpleProfiler();
        reporter = new ProfilerReporterImpl(profiler);
    }

    @After
    public void tearDown() {
        reporter.close();
    }

    @Test
    public void callStopWithoutParams() {
        ProfiledCall call = profiler.startProfiledCall("Test");
        //someMethod()
        call.stop();

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCount());
        assertEquals(1, report.getPayloadTotal());
        assertTrue("report time is not correct: " + report, report.getReportingTime() < 1000);
    }

    @Test
    public void callStopWithParams() {
        ProfiledCall call = profiler.startProfiledCall("Test");
        //someMethod()
        call.stop(30);

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCount());
        assertEquals(30, report.getPayloadTotal());
    }

    @Test
    public void buildReportWithRegexp() {
        ProfiledCall call = profiler.startProfiledCall("TestRE");
        //someMethod()
        call.stop(30);

        List<Pattern> reList = new ArrayList<Pattern>();
        reList.add(Pattern.compile(".*RE"));
        ProfilerCallReport report = getCallReport(
                reporter.buildReportAndReset(reList));

        assertEquals(1, report.getCallsCount());
        assertEquals(30, report.getPayloadTotal());
    }

    @Test
    public void buildReportWithRegexpFail() {
        ProfiledCall call = profiler.startProfiledCall("TestR_E");
        //someMethod()
        call.stop(30);

        List<Pattern> reList = new ArrayList<Pattern>();
        reList.add(Pattern.compile(".*RE"));
        ProfilerReport profilerReport = reporter.buildReportAndReset(reList);
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(profilerReport.getProfilerCallReports().size(), 0);
    }

    private ProfilerCallReport getCallReport(ProfilerReport profilerReport) {
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        return profilerReport.getProfilerCallReports().get(0);
    }
}
