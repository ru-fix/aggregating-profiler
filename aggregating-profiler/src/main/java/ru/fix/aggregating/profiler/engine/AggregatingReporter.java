package ru.fix.aggregating.profiler.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AggregatingReporter implements ProfilerReporter {
    private static final Logger log = LoggerFactory.getLogger(AggregatingReporter.class);


    private final Map<Identity, CallAggregate> sharedCounters = new ConcurrentHashMap<>();

    private final AggregatingProfiler profiler;

    private final AtomicLong lastReportTimestamp;

    private final AtomicInteger numberOfLongestActiveCallsToTrack;
    private final AtomicLong staleTimeoutAfterWhichProfiledCallAggregatedWillBeRemoved;
    private final ClosingCallback closingCallback;
    private final PercentileSettings percentileSettings;
    private volatile LabelSticker labelSticker;


    public AggregatingReporter(AggregatingProfiler profiler,
                               AtomicInteger numberOfLongestActiveCallsToTrack,
                               AtomicLong staleTimeoutAfterWhichProfiledCallAggregatedWillBeRemoved,
                               PercentileSettings percentileSettings,
                               ClosingCallback closingCallback,
                               LabelSticker labelSticker) {
        this.profiler = profiler;
        this.numberOfLongestActiveCallsToTrack = numberOfLongestActiveCallsToTrack;
        this.staleTimeoutAfterWhichProfiledCallAggregatedWillBeRemoved = staleTimeoutAfterWhichProfiledCallAggregatedWillBeRemoved;

        this.closingCallback = closingCallback;
        this.labelSticker = labelSticker;
        this.percentileSettings = percentileSettings;
        lastReportTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public void setLabelSticker(LabelSticker labelSticker) {
        Objects.requireNonNull(labelSticker);
        this.labelSticker = labelSticker;

        this.sharedCounters.forEach((identity, aggregate) ->
                labelSticker.buildLabels(identity.getName()).forEach(aggregate::setAutoLabel));

        profiler.getIndicators().forEach((indicatorIdentity, indicatorProvider) ->
                labelSticker.buildLabels(indicatorIdentity.getName()).forEach(indicatorProvider::setAutoLabel));
    }

    public void updateCallAggregates(Identity callIdentity, Consumer<CallAggregate> updateAction) {
        CallAggregate sharedCounter = sharedCounters.computeIfAbsent(
                callIdentity,
                key -> {
                    CallAggregate aggregate = new CallAggregate(
                            callIdentity,
                            numberOfLongestActiveCallsToTrack,
                            percentileSettings);
                    labelSticker.buildLabels(callIdentity.getName()).forEach(aggregate::setAutoLabel);
                    return aggregate;
                });

        updateAction.accept(sharedCounter);
    }

    @Override
    public ProfilerReport buildReportAndReset() {
        return buildReportAndReset(Optional.empty());
    }

    @Override
    public ProfilerReport buildReportAndReset(ReportFilter reportFilter) {
        return buildReportAndReset(Optional.of(reportFilter));
    }


    private ProfilerReport buildReportAndReset(Optional<ReportFilter> reportFilter) {
        long timestamp = System.currentTimeMillis();
        long spentTime = timestamp - lastReportTimestamp.getAndSet(timestamp);


        Stream<Map.Entry<Identity, AggregatingIndicationProvider>> indicatorsStream = profiler.getIndicators()
                .entrySet()
                .stream();

        if (reportFilter.isPresent()) {
            indicatorsStream = indicatorsStream.filter(
                    entry -> reportFilter.get().filter(
                            entry.getKey(),
                            entry.getValue().getAutoLabels()
                    )
            );
        }

        Map<Identity, Long> indicators = indicatorsStream
                .map(entry -> {
                    Identity name = entry.getKey();
                    try {
                        return new SimpleEntry<>(name, entry.getValue().getProvider().get());
                    } catch (Exception ex) {
                        log.error("Retrieve value for "
                                        + entry.getKey()
                                        + " failed with '"
                                        + ex.getMessage()
                                        + "'",
                                ex);
                        return new SimpleEntry<Identity, Long>(name, null);
                    }
                })
                .filter(entry -> entry.getValue() != null)
                .collect(
                        Collectors.toMap(
                                SimpleEntry::getKey,
                                SimpleEntry::getValue));

        List<ProfiledCallReport> collect = new ArrayList<>();

        for (Iterator<Map.Entry<Identity, CallAggregate>> iterator = sharedCounters.entrySet().iterator();
             iterator.hasNext(); ) {

            Map.Entry<Identity, CallAggregate> entry = iterator.next();
            CallAggregate callAggregate = entry.getValue();

            if (reportFilter.isPresent() && !reportFilter.get().filter(entry.getKey(), callAggregate.getAutoLabels())) {
                continue;
            }

            ProfiledCallReport counterReport = callAggregate.buildReportAndReset(spentTime);

            // Skip and remove empty counter that was not accessed for a long time.
            // There are always a lot of ProfiledCalls that are rarely active, or active only during short period of time
            // during application startup or first launch of long running tasks.
            // Removing empty aggregates reduce amount of memory consumed by profiler in this cases.
            //
            // TODO: We can lose some metrics
            // Edge case when we can lose metric: Metric was not active for a long time and then suddenly became active.
            // Reporter sees empty CallAggregate that was not accessed for a long time and removes it from map.
            // And there is a ProfiledCall that already obtained reference to this CallAggregate.
            // But not yet incremented counters in it.
            // ProfiledCall is not aware that he holds reference to CallAggregate that was removed from sharedCounters.
            // ProfiledCall will increment counters in CallAggregate that will be destroyed by GC,
            // and will never be accessed by ProfilerReporter.
            // As a result we lose metric.

            // We need to check if stamped lock could help us not to lose performance
            // and in a same time save us in this situation

            boolean noActiveCalls = counterReport.getActiveCallsCountMax() == 0;
            boolean noCallsBetweenPreviousAndCurrentReporting = counterReport.getStopSum() == 0;
            long lastAccessTimestamp = callAggregate.lastAccessTimestamp.get();
            boolean wasAccessedAtLeastOnce = lastAccessTimestamp != 0;
            boolean noCallsForALongTime = (System.currentTimeMillis() - lastAccessTimestamp) >
                    staleTimeoutAfterWhichProfiledCallAggregatedWillBeRemoved.get();

            if (noActiveCalls &&
                    noCallsBetweenPreviousAndCurrentReporting &&
                    wasAccessedAtLeastOnce &&
                    noCallsForALongTime) {

                iterator.remove();
                continue;
            }

            collect.add(counterReport);
        }


        collect.sort(Comparator.comparing(report -> report.getIdentity().getName()));

        return new ProfilerReport(indicators, collect);
    }

    @Override
    public void close() {
        closingCallback.closed();
    }

    public void onIndicatorAttached(Identity identity, AggregatingIndicationProvider provider) {
        labelSticker.buildLabels(identity.getName()).forEach(provider::setAutoLabel);
    }
}
