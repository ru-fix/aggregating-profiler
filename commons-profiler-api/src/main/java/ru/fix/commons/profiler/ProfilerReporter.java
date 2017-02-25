package ru.fix.commons.profiler;

public interface ProfilerReporter extends AutoCloseable {

    ProfilerReport buildReportAndReset();
}
