package ru.fix.aggregating.profiler;

import java.util.List;

public interface ProfilerReporter extends AutoCloseable, Tagging {

    ProfilerReport buildReportAndReset();

    /**
     * @return empty report in case of empty patterns
     */
    ProfilerReport buildReportAndReset(String tag);
}
