package ru.fix.commons.profiler;

import ru.fix.commons.profiler.impl.TrimmedDotsString;

/**
 * Attach fixed prefix to profiled calls and indicator names
 * @author Kamil Asfandiyarov
 */
public class PrefixedProfiler implements Profiler {
    private final Profiler profiler;
    private final TrimmedDotsString profilerPrefix;

    public PrefixedProfiler(Profiler profiler, String profilerPrefix) {
        this.profiler = profiler;
        this.profilerPrefix = new TrimmedDotsString(profilerPrefix);
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(profilerPrefix + "." + new TrimmedDotsString(name));
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(profilerPrefix + "." + new TrimmedDotsString(name), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(profilerPrefix + "." + new TrimmedDotsString(name));
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
