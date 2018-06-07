package ru.fix.commons.profiler.impl;

import ru.fix.commons.profiler.IndicationProvider;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.Profiler;
import ru.fix.commons.profiler.ProfilerReporter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;


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

    @Override
    public <T> CompletableFuture<T> profiledCall(String name, Supplier<CompletableFuture<T>> cfSupplier) {
        ProfiledCall call = startProfiledCall(name);
        try {
            return cfSupplier.get()
                    .whenComplete((res, thr) -> call.stop());
        } catch (Exception e) {
            call.cancel();
            throw e;
        }
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

    @Override
    public ProfilerReporter createReporter(
            boolean enableActiveCallsMaxLatency,
            int activeCallsToKeepBetweenReports) {
        return new ProfilerReporterImpl(this, enableActiveCallsMaxLatency, activeCallsToKeepBetweenReports);
    }
}
