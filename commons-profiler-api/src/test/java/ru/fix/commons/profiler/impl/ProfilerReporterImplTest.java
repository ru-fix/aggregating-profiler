package ru.fix.commons.profiler.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;

import java.util.*;
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

        assertEquals(1, report.getCallsCountSum());
        assertEquals(1, report.getPayloadSum());
        assertTrue(report.getReportingTimeAvg() < 1000, "report time is not correct: " + report);
    }

    @Test
    public void callStopWithParams() {
        ProfiledCall call = profiler.start("Test");
        //someMethod()
        call.stop(30);

        ProfilerCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCountSum());
        assertEquals(30, report.getPayloadSum());
    }

    @Test
    public void buildReportWithDefault() {
        ProfiledCall call = profiler.start("test");
        call.stop(30);

        ProfilerCallReport report = getCallReport(
                reporter.buildReportAndReset("default"));

        assertEquals(30, report.getPayloadSum());
    }

    @Test
    public void buildReportWithSharedCounterTag() {
        Map<String, Set<Pattern>> separator = new HashMap<>();
        separator.put("tag", new HashSet<Pattern>());
        separator.get("tag").add(Pattern.compile(".*test.*"));
        profiler.setGroupsSeparator(separator);
        ProfiledCall call = profiler.start("test");
        call.stop(30);

        ProfilerReport profilerReport = reporter.buildReportAndReset("tag");
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(profilerReport.getProfilerCallReports().size(), 0);
    }

    @Test
    public void buildReportWithIndicatorTag() {
        Map<String, Set<Pattern>> separator = new HashMap<>();
        separator.put("tag", new HashSet<Pattern>());
        separator.get("tag").add(Pattern.compile(".*nop.*"));
        reporter.setGroupsSeparator(separator);
        profiler.attachIndicator("nop", () -> new Long(10));
        ProfiledCall call = profiler.start("test");
        call.stop(30);

        ProfilerReport profilerReport = reporter.buildReportAndReset("tag");
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(profilerReport.getProfilerCallReports().size(), 0);
    }

    private ProfilerCallReport getCallReport(ProfilerReport profilerReport) {
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        return profilerReport.getProfilerCallReports().get(0);
    }
}
