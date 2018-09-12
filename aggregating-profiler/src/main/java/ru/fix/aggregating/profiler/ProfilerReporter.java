package ru.fix.aggregating.profiler;

import java.util.List;

public interface ProfilerReporter extends AutoCloseable {
    ProfilerReport buildReportAndReset();

    /**
     * @return empty report in case of nonexistent tag
     */
    ProfilerReport buildReportAndReset(String tagName);
    ProfilerReport buildReportAndReset(String tagName, String tagValue);
}
