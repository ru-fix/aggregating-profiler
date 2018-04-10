package ru.fix.commons.profiler;

/**
 * Attach fixed prefix to profiled calls and indicator names
 * @author Kamil Asfandiyarov
 */
public class PrefixedProfiler implements Profiler {
    private final Profiler profiler;
    private final String profilerPrefix;

    public PrefixedProfiler(Profiler profiler, String profilerPrefix) {
        this.profiler = profiler;
        this.profilerPrefix = profilerPrefix;
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(profilerPrefix + name);
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(profilerPrefix + name, indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(profilerPrefix + name);
    }

    @Override
    public ProfilerReporter createReporter() {
        return profiler.createReporter();
    }

    @Override
    public ProfilerReporter createReporter(boolean enableActiveCallsMaxLatency, int activeCallsToKeepBetweenReports) {
        return profiler.createReporter(enableActiveCallsMaxLatency, activeCallsToKeepBetweenReports);
    }
}
