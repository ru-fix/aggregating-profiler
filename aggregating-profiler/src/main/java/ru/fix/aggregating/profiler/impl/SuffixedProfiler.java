package ru.fix.aggregating.profiler.impl;

import ru.fix.aggregating.profiler.IndicationProvider;
import ru.fix.aggregating.profiler.ProfiledCall;
import ru.fix.aggregating.profiler.Profiler;
import ru.fix.aggregating.profiler.ProfilerReporter;
import ru.fix.aggregating.profiler.util.NameNormalizer;

public class SuffixedProfiler implements Profiler {

    private final Profiler profiler;

    private final String normalizedName;

    public SuffixedProfiler(Profiler profiler, String name) {
        this.profiler = profiler;
        this.normalizedName = NameNormalizer.trimDots(name);
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(normalizedName + "." + NameNormalizer.trimDots(name));
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(normalizedName + "." + NameNormalizer.trimDots(name), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(normalizedName + "." + NameNormalizer.trimDots(name));
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