package ru.fix.commons.profiler.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 26.12.17.
 */
public class ProfilerReporterImplTest {

    private SimpleProfiler profiler;
    private ProfilerReporterImpl reporter;

    @BeforeEach
    public void setup() {
        profiler = new SimpleProfiler();
        reporter = new ProfilerReporterImpl(profiler);
    }

    @AfterEach
    public void tearDown() {
        reporter.close();
    }

    @Test
    public void callStopWithoutParams() {
        ProfiledCall call = profiler.start("Test");
        //someMethod()
        call.stop();

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCount());
        assertEquals(1, report.getPayloadTotal());
        assertTrue(report.getReportingTime() < 1000, "report time is not correct: " + report);
    }

    @Test
    public void callStopWithParams() {
        ProfiledCall call = profiler.start("Test");
        //someMethod()
        call.stop(30);

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCount());
        assertEquals(30, report.getPayloadTotal());
    }

    @Test
    public void buildReportWithRegexp() {
        ProfiledCall call = profiler.start("TestRE");
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
        ProfiledCall call = profiler.start("TestR_E");
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
