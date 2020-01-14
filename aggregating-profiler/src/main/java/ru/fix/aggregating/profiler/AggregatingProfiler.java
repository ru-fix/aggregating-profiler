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

    //TODO: move indicators to reporters, each reporter will set is's own auto tags
    private final Map<Identity, AggregatingIndicationProvider> indicators = new ConcurrentHashMap<>();
    private volatile LabelSticker labelSticker = new NoopLabelSticker();

    private final PercentileSettings percentileSettings;

    public AggregatingProfiler(PercentileSettings percentileSettings) {
        this.percentileSettings = percentileSettings;
    }

    public AggregatingProfiler() {
        this(new PercentileSettings());
    }

    /**
     * if 0 then tracking uncompleted profiled calls is disabled
     */
    private final AtomicInteger numberOfLongestActiveCallsToTrack = new AtomicInteger(10);

    public ProfiledCall profiledCall(String name) {
        return profiledCall(new Identity(name));
    }

    public ProfiledCall profiledCall(Identity identity) {
        return new AggregatingCall(
                identity,
                (profiledCallName, updateAction) ->
                        profilerReporters.forEach(
                                reporter ->
                                        reporter.updateCallAggregates(profiledCallName, updateAction)
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
        String normalizedName = NameNormalizer.trimDots(name);
        attachIndicator(new Identity(normalizedName), indicationProvider);
    }

    @Override
    public void attachIndicator(Identity identity, IndicationProvider indicationProvider) {
        AggregatingIndicationProvider provider = new AggregatingIndicationProvider(indicationProvider);
        indicators.put(identity, provider);

        //TODO: call back will be replaced by direct Reporter::attachIndicator invocation
        // Each report will have it's own indicator provider with populated auto labels.
        for (AggregatingReporter reporter : profilerReporters){
            reporter.onIndicatorAttached(identity, provider);
        }
    }

    @Override
    public void detachIndicator(String name) {
        String normalizedName = NameNormalizer.trimDots(name);
        detachIndicator(new Identity(normalizedName));
    }

    @Override
    public void detachIndicator(Identity identity) {
        indicators.remove(identity);
    }



    public Map<Identity, AggregatingIndicationProvider> getIndicators() {
        return indicators;
    }

    @Override
    public ProfilerReporter createReporter() {
        AggregatingReporter[] reporter = new AggregatingReporter[1];
        reporter[0] = new AggregatingReporter(
                this,
                numberOfLongestActiveCallsToTrack,
                percentileSettings,
                () -> this.unregisterReporter(reporter[0]),
                new NoopLabelSticker());
        reporter[0].setLabelSticker(labelSticker);
        this.registerReporter(reporter[0]);
        return reporter[0];
    }

    /**
     * @param numberOfLongestActiveCallsToTrack if 0 then tracking uncompleted profiled calls is disabled
     */
    public AggregatingProfiler setNumberOfLongestActiveCallsToTrack(
            int numberOfLongestActiveCallsToTrack) {
        this.numberOfLongestActiveCallsToTrack.set(numberOfLongestActiveCallsToTrack);
        return this;
    }

    public int getNumberOfLongestActiveCallsToTrack(){
        return numberOfLongestActiveCallsToTrack.get();
    }
}
