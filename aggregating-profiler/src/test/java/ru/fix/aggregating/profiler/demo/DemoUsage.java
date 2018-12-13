package ru.fix.aggregating.profiler.demo;

import org.junit.jupiter.api.Test;
import ru.fix.aggregating.profiler.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.fix.aggregating.profiler.ReportFilters.containsLabel;
import static ru.fix.aggregating.profiler.ReportFilters.notContainsLabel;

/**
 * This example shows how to use profiler and how to collect data
 */
public class DemoUsage {

    @Test
    void howToCallProfilerAndObtainsReport() throws Exception {
        Profiler profiler = new AggregatingProfiler();

        // start collecting data from profilers
        try (ProfilerReporter reporter = profiler.createReporter()) {

            // we want to mesure call.name calls count
            profiler.profiledCall("call.name").call();

            // stop collection data
            ProfilerReport report = reporter.buildReportAndReset();

            // report contains only one metric
            assertEquals(1, report.getProfilerCallReports().size());
            ProfiledCallReport profiledCallReport = report.getProfilerCallReports().get(0);

            // "call.name" was called only once
            assertEquals("call.name", profiledCallReport.getIdentity().getName());
            assertEquals(1, profiledCallReport.getCallsCountSum());
        }
    }

    /**
     * This example shows us how to deal with tags.
     * Tags allows us group calls and when we build report it build reports only for
     * metrics in this group
     *
     * @throws Exception
     */
    @Test
    void howToDealWithTags() throws Exception {
        // if metric contains "1" we move it to separate group
        String testTag = "testTag";
        Map<String, Set<Pattern>> tagRules = new HashMap<>();
        tagRules.put("contains_number_1", new HashSet<>());
        tagRules.get("contains_number_1").add(Pattern.compile(".*1.*"));
        LabelSticker labelSticker = new RegexpLabelSticker(testTag, tagRules);

        Profiler profiler = new AggregatingProfiler();
        profiler.setLabelSticker(labelSticker);

        try (ProfilerReporter reporter = profiler.createReporter()) {

            // first call must go to special group
            // second call must go to default group
            profiler.profiledCall("call1.name").call();
            profiler.profiledCall("call2.name").call();

            // report for special group
            ProfilerReport reportContains1 = reporter.buildReportAndReset(containsLabel(testTag, "contains_number_1"));
            assertEquals(1, reportContains1.getProfilerCallReports().size());
            ProfiledCallReport profiledCallReport = reportContains1.getProfilerCallReports().get(0);
            assertEquals("call1.name", profiledCallReport.getIdentity().getName());
            assertEquals(1, profiledCallReport.getCallsCountSum());

            // report for default group
            ProfilerReport reportDefaults = reporter.buildReportAndReset(notContainsLabel(testTag));
            assertEquals(1, reportDefaults.getProfilerCallReports().size());
            ProfiledCallReport profiledCallReportDefault = reportDefaults.getProfilerCallReports().get(0);
            assertEquals("call2.name", profiledCallReportDefault.getIdentity().getName());
            assertEquals(1, profiledCallReportDefault.getCallsCountSum());

            // report for all, ignore group
            profiler.profiledCall("call1.name").call();
            profiler.profiledCall("call2.name").call();
            ProfilerReport reportBackward = reporter.buildReportAndReset();
            assertEquals(2, reportBackward.getProfilerCallReports().size());
        }
    }
}
