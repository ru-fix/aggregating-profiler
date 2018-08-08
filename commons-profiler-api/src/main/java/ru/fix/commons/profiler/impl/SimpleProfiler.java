package ru.fix.commons.profiler.impl;

import ru.fix.commons.profiler.IndicationProvider;
import ru.fix.commons.profiler.IndicationProviderTag;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.Profiler;
import ru.fix.commons.profiler.ProfilerReporter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author Kamil Asfandiyarov
 */
public class SimpleProfiler implements Profiler {

    private static final String INDICATOR_SUFFIX = ".indicatorMax";

    private final CopyOnWriteArrayList<ProfilerReporterImpl> profilerReporters = new CopyOnWriteArrayList<>();

    private final Map<String, IndicationProviderTag> indicators = new ConcurrentHashMap<>();
    private final Map<String, Set<Pattern>> groupSeparator = new ConcurrentHashMap<>();
    
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
        IndicationProviderTag tag = new IndicationProviderTag(indicationProvider);
        String newName = normalizeIndicatorName(name);
        tag.evalGroupTag(newName, groupSeparator);
        indicators.put(newName,tag);
    }

    @Override
    public void detachIndicator(String name) {
        indicators.remove(normalizeIndicatorName(name));
    }

    private static String normalizeIndicatorName(String name) {
        if (!name.endsWith(INDICATOR_SUFFIX)) {
            name = name.concat(INDICATOR_SUFFIX);
        }
        return name;
    }

    void applyToSharedCounters(String profiledCallName, Consumer<SharedCounters> consumer) {
        profilerReporters.forEach(reporter -> reporter.applyToSharedCounters(profiledCallName, consumer));
    }

    Map<String, IndicationProviderTag> getIndicators() {
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

    public void setGroupsSeparator(Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator.clear();
        this.groupSeparator.putAll(groupSeparator);
        this.indicators.forEach((k, v) -> v.evalGroupTag(k, groupSeparator));
    }
}
