package ru.fix.commons.profiler;

import java.util.regex.Pattern;
import java.util.List;

public interface ProfilerReporter extends AutoCloseable {

    /**
     * Enable active calls max latency metric. Enable this metric to see if any calls hangup.
     *
     * @return previous value
     */
    boolean setEnableActiveCallsMaxLatency(boolean enable);

    /**
     * Specifies the size of top longest active calls that will be kept on metrics reset.
     *
     * @return previous value
     */
    int setNumberOfActiveCallsToKeepBetweenReports(int number);

    ProfilerReport buildReportAndReset();
    ProfilerReport buildReportAndReset(List<Pattern> patterns);
}
