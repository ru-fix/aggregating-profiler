package ru.fix.aggregating.profiler;

import java.util.List;

public interface ProfilerReporter extends AutoCloseable {

    ProfilerReport buildReportAndReset();

    /**
     * @return empty report in case of empty patterns
     */
    ProfilerReport buildReportAndReset(String tagName, String tagValue);
}
