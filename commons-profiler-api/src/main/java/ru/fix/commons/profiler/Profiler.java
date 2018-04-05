package ru.fix.commons.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public interface Profiler {

    /**
     * @param name Name of profiling call (e.g name of method about to be profiled)
     *             Method name could be separated by dot '.'
     */
    ProfiledCall profiledCall(String name);

    /**
     * Add named indicator to profiler.
     *
     * @param name Name of indicator
     *             Indicator name could be separated by dot '.'
     * @param indicationProvider Indicator value provider. Must be thread-safe.
     */
    void attachIndicator(String name, IndicationProvider indicationProvider);

    /**
     * Remove indicator
     *
     * @param name Name of indicator
     */
    void detachIndicator(String name);

    /**
     * Create new instance of reporter.
     * Reporter is closable resource
     */
    ProfilerReporter createReporter();

    /**
     * Create new instance of reporter.
     * Reporter is closable resource.
     *
     * @param enableActiveCallsMaxLatency     see {@link ProfilerReporter#setEnableActiveCallsMaxLatency(boolean)}
     * @param activeCallsToKeepBetweenReports see {@link ProfilerReporter#setNumberOfActiveCallsToKeepBetweenReports(int)}
     */
    ProfilerReporter createReporter(boolean enableActiveCallsMaxLatency, int activeCallsToKeepBetweenReports);
}
