package ru.fix.aggregating.profiler.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.fix.aggregating.profiler.*;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 26.12.17.
 */
public class AggregatingReporterTest {

    private AggregatingProfiler profiler;
    private ProfilerReporter reporter;

    @BeforeEach
    public void setup() {
        profiler = new AggregatingProfiler();
        reporter = profiler.createReporter();
    }

    @AfterEach
    public void tearDown() throws Exception {
        reporter.close();
    }

    @Test
    public void callStopWithoutParams() {
        ProfiledCall call = profiler.start("Test");
        //someMethod()
        call.stop();

        ProfiledCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCountSum());
        assertEquals(1, report.getPayloadSum());
        assertTrue(report.getReportingTimeAvg() < 1000, "report time is not correct: " + report);
    }

    @Test
    public void callStopWithParams() {
        ProfiledCall call = profiler.start("Test");
        //someMethod()
        call.stop(30);

        ProfiledCallReport report = getCallReport(reporter.buildReportAndReset());

        assertEquals(1, report.getCallsCountSum());
        assertEquals(30, report.getPayloadSum());
    }

    @Test
    public void regexpTaggerBuildReportWithSharedCounterTag() {
        Map<String, Set<Pattern>> separator = new HashMap<>();
        String testTag = "testTag";
        separator.put("tag", new HashSet<Pattern>());
        separator.get("tag").add(Pattern.compile(".*test.*"));
        profiler.setTagger(new RegexpTagger(testTag, separator));
        reporter = profiler.createReporter();
        ProfiledCall call = profiler.start("test");
        call.stop(30);

        ProfilerReport profilerReport = reporter.buildReportAndReset(testTag, "tag");
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
    }

    @Test
    public void regexpTaggerBuildReportWithIndicatorTag() {
        Map<String, Set<Pattern>> separator = new HashMap<>();
        String testTag = "testTag";
        separator.put("tag", new HashSet<Pattern>());
        separator.get("tag").add(Pattern.compile(".*nop.*"));
        profiler.setTagger(new RegexpTagger(testTag, separator));
        profiler.attachIndicator("nop", () -> new Long(10));
        ProfiledCall call = profiler.start("test");
        call.stop(30);
        
        reporter = profiler.createReporter();
        ProfilerReport profilerReport = reporter.buildReportAndReset(testTag, "tag");

        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(0, profilerReport.getProfilerCallReports().size());
        assertEquals(1, profilerReport.getIndicators().size());
    }

    @Test
    public void changeTaggerReassignTagsBuildOldReportForNewTag() {
        Map<String, Set<Pattern>> separator = new HashMap<>();
        String testTag = "testTag";
        separator.put("tag", new HashSet<Pattern>());
        separator.get("tag").add(Pattern.compile(".*nop.*"));
        profiler.setTagger(new RegexpTagger(testTag, separator));
        profiler.attachIndicator("nop", () -> new Long(10));
        ProfiledCall call = profiler.start("test");
        call.stop(30);

        separator = new HashMap<>();
        separator.put("tag1", new HashSet<Pattern>());
        separator.get("tag1").add(Pattern.compile(".*nop.*"));
        profiler.setTagger(new RegexpTagger(testTag, separator));
        reporter = profiler.createReporter();
        ProfilerReport profilerReport = reporter.buildReportAndReset(testTag, "tag1");

        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(0, profilerReport.getProfilerCallReports().size());
        assertEquals(1, profilerReport.getIndicators().size());
    }

    private ProfiledCallReport getCallReport(ProfilerReport profilerReport) {
        assertNotNull(profilerReport.getProfilerCallReports());
        assertEquals(1, profilerReport.getProfilerCallReports().size());
        return profilerReport.getProfilerCallReports().get(0);
    }
}
