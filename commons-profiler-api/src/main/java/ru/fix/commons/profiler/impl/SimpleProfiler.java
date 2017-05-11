package ru.fix.commons.profiler.impl;

import ru.fix.commons.profiler.IndicationProvider;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.Profiler;
import ru.fix.commons.profiler.ProfilerReporter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


/**
 * @author Kamil Asfandiyarov
 */
public class SimpleProfiler implements Profiler {
    private final CopyOnWriteArrayList<ProfilerReporterImpl> profilerReporters = new CopyOnWriteArrayList<>();

    private final Map<String, IndicationProvider> indicators = new ConcurrentHashMap<>();

    @Override
    public ProfiledCall profiledCall(String name) {
        return new ProfiledCallImpl(this, name);
    }

    public SimpleProfiler registerReporter(ProfilerReporterImpl reporter) {
        profilerReporters.add(reporter);
        return this;
    }

    public SimpleProfiler unregisterReporter(ProfilerReporterImpl reporter) {
        profilerReporters.remove(reporter);
        return this;
    }


    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        indicators.put(name, indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        indicators.remove(name);
    }

    void applyToSharedCounters(String profiledCallName, Consumer<SharedCounters> consumer) {
        profilerReporters.forEach(reporter -> reporter.applyToSharedCounters(profiledCallName, consumer));
    }

    Map<String, IndicationProvider> getIndicators() {
        return indicators;
    }

    @Override
    public ProfilerReporter createReporter() {
        return new ProfilerReporterImpl(this);
    }
}
