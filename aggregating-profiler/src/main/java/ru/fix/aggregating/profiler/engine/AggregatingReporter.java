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
    private final ClosingCallback closingCallback;
    private final PercentileSettings percentileSettings;
    private volatile LabelSticker labelSticker;

    public AggregatingReporter(AggregatingProfiler profiler,
                               AtomicInteger numberOfLongestActiveCallsToTrack,
                               PercentileSettings percentileSettings,
                               ClosingCallback closingCallback,
                               LabelSticker labelSticker) {
        this.profiler = profiler;
        this.numberOfLongestActiveCallsToTrack = numberOfLongestActiveCallsToTrack;
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
        updateAction.accept(
                sharedCounters.computeIfAbsent(
                        callIdentity,
                        key -> {
                            CallAggregate aggregate = new CallAggregate(
                                    callIdentity,
                                    numberOfLongestActiveCallsToTrack,
                                    percentileSettings);
                            labelSticker.buildLabels(callIdentity.getName()).forEach(aggregate::setAutoLabel);
                            return aggregate;
                        }));
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

            if (reportFilter.isPresent() && !reportFilter.get().filter(entry.getKey(), entry.getValue().getAutoLabels())) {
                continue;
            }

            ProfiledCallReport counterReport = entry.getValue().buildReportAndReset(spentTime);

            // Skip and remove empty counter
            // There are always a lot of ProfiledCalls that are rarely active, or active only during short period of time
            // during application startup or scheduled tasks.
            // Removing empty aggregates reduce amount of memory consumed by profiler in such cases.
            //
            // TODO: We can lose some metrics
            // Reporter sees empty CallAggregate and removed it from map.
            // There is a ProfiledCall that already obtained reference to this CallAggregate,
            // but not yet incremented counters in it.
            // ProfiledCall will increment counters in CallAggregate that will be destroyed by GC,
            // and will never be accessed by ProfilerReporter
            if (counterReport.getStopSum() == 0 && counterReport.getActiveCallsCountMax() == 0) {
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
