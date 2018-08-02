package ru.fix.aggregating.profiler;

public interface ProfilerReporter extends AutoCloseable {

    ProfilerReport buildReportAndReset();
}
