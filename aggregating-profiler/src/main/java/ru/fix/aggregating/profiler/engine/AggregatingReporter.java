package ru.fix.aggregating.profiler.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.AggregatingProfiler;
import ru.fix.aggregating.profiler.ProfiledCallReport;
import ru.fix.aggregating.profiler.ProfilerReport;
import ru.fix.aggregating.profiler.ProfilerReporter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AggregatingReporter implements ProfilerReporter {
    private static final Logger log = LoggerFactory.getLogger(AggregatingReporter.class);

    private final Map<String, CallAggregate> sharedCounters = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    // update counters lock
    private final Lock readLock = readWriteLock.readLock();
    // create report and then reset counters lock
    private final Lock writeLock = readWriteLock.writeLock();

    private final AggregatingProfiler profiler;

    private final AtomicLong lastReportTimestamp;

    private final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports;
    private final ClosingCallback closingCallback;


    public AggregatingReporter(AggregatingProfiler profiler,
                               AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports,
                               ClosingCallback closingCallback) {
        this.profiler = profiler;
        this.numberOfActiveCallsToTrackAndKeepBetweenReports = numberOfActiveCallsToTrackAndKeepBetweenReports;
        this.closingCallback = closingCallback;
        lastReportTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public void updateCallAggregates(String profiledCallName, Consumer<CallAggregate> updateAction) {
        updateAction.accept(
                sharedCounters.computeIfAbsent(profiledCallName, key ->
                        new CallAggregate(profiledCallName, numberOfActiveCallsToTrackAndKeepBetweenReports)
                ));
    }

    @Override
    public ProfilerReport buildReportAndReset() {
        return buildReportAndReset(Optional.empty());
    }

    @Override
    public ProfilerReport buildReportAndReset(String tag) {
        return buildReportAndReset(Optional.ofNullable(tag));
    }

    private ProfilerReport buildReportAndReset(Optional<String> tag) {
        long timestamp = System.currentTimeMillis();
        long spentTime = timestamp - lastReportTimestamp.getAndSet(timestamp);

        Map<String, Long> indicators = profiler.getIndicators()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().getTagValue().equals(tag.get()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> {
                            try {
                                return e.getValue().getProvider().get();
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                            }
                            return null;
                        }));

        List<ProfiledCallReport> collect = new ArrayList<>();

        for (Iterator<Map.Entry<String, CallAggregate>> iterator = sharedCounters.entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<String, CallAggregate> entry = iterator.next();
            if (patterns.isPresent() && !patterns.get()
                    .stream()
                    .anyMatch(p -> p.matcher(entry.getKey()).matches())) {
                continue;
            }
            ProfiledCallReport counterReport = entry.getValue().buildReportAndReset(spentTime);

            // Skip and remove empty counter
            // There are always a lot of ProfiledCalls that are rare active, or active only short period of time
            // during application startup or scheduled tasks.
            // Removing empty aggregates reduce amount of memory consumed by profiler in such cases.
            //
            // TODO: We can lose some metrics
            // Reporter sees empty CallAggregate and removed it from map.
            // There is a ProfiledCall that already obtained reference to this CallAggregate,
            // but not yet incremented counters in it.
            // ProfiledCall will increment counters in CallAggregate that will be destroyed by GC,
            // and will never be accessed by ProfilerReporter
            if (counterReport.getCallsCountSum() == 0 && counterReport.getActiveCallsCountMax() == 0) {
                iterator.remove();
                continue;
            }

            collect.add(counterReport);
        }


        collect.sort(Comparator.comparing(ProfiledCallReport::getName));

        return new ProfilerReport(indicators, collect);
    }

    @Override
    public void close() {
        closingCallback.closed();
    }
}
