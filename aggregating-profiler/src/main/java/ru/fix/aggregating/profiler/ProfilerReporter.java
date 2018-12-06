package ru.fix.aggregating.profiler;

public interface ProfilerReporter extends AutoCloseable {
    ProfilerReport buildReportAndReset();

    /**
     * @return empty report in case of nonexistent tag
     */
    ProfilerReport buildReportAndReset(String tagName, String tagValue);
}
