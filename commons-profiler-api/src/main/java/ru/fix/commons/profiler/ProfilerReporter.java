package ru.fix.commons.profiler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    /**
     * @return empty report in case of empty patterns
     */
    ProfilerReport buildReportAndReset(String tag);

    void setGroupsSeparator(Map<String, Set<Pattern>> groupSeparator);
}
