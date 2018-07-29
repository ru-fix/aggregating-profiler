package ru.fix.aggregating.profiler;

import ru.fix.aggregating.profiler.engine.AggregatingCall;
import ru.fix.aggregating.profiler.engine.AggregatingReporter;
import ru.fix.aggregating.profiler.engine.NameNormalizer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Kamil Asfandiyarov
 */
public class AggregatingProfiler implements Profiler {

    private final CopyOnWriteArrayList<AggregatingReporter> profilerReporters = new CopyOnWriteArrayList<>();

    private final Map<String, IndicationProvider> indicators = new ConcurrentHashMap<>();

    /**
     * if 0 then tracking uncompleted profiled calls is disabled
     */
    private final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports = new AtomicInteger(10);

    @Override
    public ProfiledCall profiledCall(String name) {
        return new AggregatingCall(
                name,
                (profiledCallName, updateAction) ->
                        profilerReporters.forEach(
                                reporter ->
                                        reporter.updateCounters(profiledCallName, updateAction)
                        )
        );
    }

    private void registerReporter(AggregatingReporter reporter) {
        profilerReporters.add(reporter);
    }

    private void unregisterReporter(AggregatingReporter reporter) {
        profilerReporters.remove(reporter);
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
        indicators.put(NameNormalizer.trimDots(name), indicationProvider);
    }

    @Override
    public void detachIndicator(String name) {
        indicators.remove(NameNormalizer.trimDots(name));
    }

    public Map<String, IndicationProvider> getIndicators() {
        return indicators;
    }

    @Override
    public ProfilerReporter createReporter() {
        AggregatingReporter[] reporter = new AggregatingReporter[1];
        reporter[0] = new AggregatingReporter(
                this,
                numberOfActiveCallsToTrackAndKeepBetweenReports,
                () -> this.unregisterReporter(reporter[0]));
        this.registerReporter(reporter[0]);
        return reporter[0];
    }

    /**
     * @param numberOfActiveCallsToTrackAndKeepBetweenReports if 0 then tracking uncompleted profiled calls is disabled
     */
    public AggregatingProfiler setNumberOfActiveCallsToTrackAndKeepBetweenReports(
            int numberOfActiveCallsToTrackAndKeepBetweenReports) {
        this.numberOfActiveCallsToTrackAndKeepBetweenReports.set(numberOfActiveCallsToTrackAndKeepBetweenReports);
        return this;
    }

    public int getNumberOfActiveCallsToTrackAndKeepBetweenReports(){
        return numberOfActiveCallsToTrackAndKeepBetweenReports.get();
    }
}
