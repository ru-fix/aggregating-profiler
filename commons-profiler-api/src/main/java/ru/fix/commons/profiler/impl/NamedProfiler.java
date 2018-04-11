package ru.fix.commons.profiler.impl;

import ru.fix.commons.profiler.IndicationProvider;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.Profiler;
import ru.fix.commons.profiler.ProfilerReporter;

public class NamedProfiler implements Profiler {

    private Profiler profiler;

    private String profilerName;

    public NamedProfiler(Profiler profiler, String name) {
        this.profiler = profiler;
        this.profilerName = trimDots(name);
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return profiler.profiledCall(profilerName + "." + trimDots(name));
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        profiler.attachIndicator(profilerName + "." + trimDots(name), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        profiler.detachIndicator(profilerName + "." + trimDots(name));
    }

    @Override
    public ProfilerReporter createReporter() {
        return profiler.createReporter();
    }

    @Override
    public ProfilerReporter createReporter(boolean enableActiveCallsMaxLatency, int activeCallsToKeepBetweenReports) {
        return profiler.createReporter(enableActiveCallsMaxLatency, activeCallsToKeepBetweenReports);
    }

    private String trimDots(String s) {
        if (s == null) {
            throw new NullPointerException("Given null instead of string");
        }
        return s.trim().replaceAll("^\\.|\\.$", "");
    }
}