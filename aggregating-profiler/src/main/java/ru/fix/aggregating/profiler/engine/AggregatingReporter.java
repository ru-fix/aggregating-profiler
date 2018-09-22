package ru.fix.aggregating.profiler.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AggregatingReporter implements ProfilerReporter {
    private static final Logger log = LoggerFactory.getLogger(AggregatingReporter.class);

    private static final String INDICATOR_SUFFIX = ".indicatorMax";

    private final Map<String, CallAggregate> sharedCounters = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final AggregatingProfiler profiler;

    private final AtomicLong lastReportTimestamp;

    private final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports;
    private final ClosingCallback closingCallback;
    private volatile Tagger tagger;

    public AggregatingReporter(AggregatingProfiler profiler,
                               AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports,
                               ClosingCallback closingCallback,
                               Tagger tagger) {
        this.profiler = profiler;
        this.numberOfActiveCallsToTrackAndKeepBetweenReports = numberOfActiveCallsToTrackAndKeepBetweenReports;
        this.closingCallback = closingCallback;
        this.tagger = tagger;
        lastReportTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public void setTagger(Tagger tagger) {
        Objects.requireNonNull(tagger);
        this.tagger = tagger;
        this.sharedCounters.forEach(tagger::assignTag);
    }

    public void updateCallAggregates(String profiledCallName, Consumer<CallAggregate> updateAction) {
        updateAction.accept(
            sharedCounters.computeIfAbsent(
                profiledCallName,
                key -> tagger.assignTag(
                    profiledCallName,
                    new CallAggregate(
                        profiledCallName,
                        numberOfActiveCallsToTrackAndKeepBetweenReports))));
    }

    @Override
    public ProfilerReport buildReportAndReset() {
        return buildReportAndReset(Optional.empty(), Optional.empty());
    }

    @Override
    public ProfilerReport buildReportAndReset(String tagName, String tagValue) {
        return buildReportAndReset(Optional.ofNullable(tagName),
                                   Optional.ofNullable(tagValue));
    }

    private ProfilerReport buildReportAndReset(Optional<String> tagName,
                                               Optional<String> tagValue) {
        long timestamp = System.currentTimeMillis();
        long spentTime = timestamp - lastReportTimestamp.getAndSet(timestamp);

        Map<String, Long> indicators = profiler.getIndicators()
                .entrySet()
                .stream()
                .filter(entry -> ! tagName.isPresent()
                        || entry.getValue().hasTag(tagName.get(), tagValue.orElse(null)))
                .filter(entry -> {
                        try {
                            if(entry.getValue().getProvider().get() != null) {
                                return true;
                            }
                            log.error("Inicator '{}' return null value", entry.getKey());
                            return false;
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                            return false;
                        }
                    })
                .collect(
                    Collectors.toMap(
                        e -> {
                            String name = e.getKey();
                            if (!name.endsWith(INDICATOR_SUFFIX)) {
                                name = name.concat(INDICATOR_SUFFIX);
                            }
                            return name;
                        },
                        e -> {
                            try {
                                return e.getValue().getProvider().get();
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                                throw new IndicationProviderValueException(ex);
                            }
                        }));

        List<ProfiledCallReport> collect = new ArrayList<>();

        for (Iterator<Map.Entry<String, CallAggregate>> iterator = sharedCounters.entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<String, CallAggregate> entry = iterator.next();
            if (tagName.isPresent() && ! entry.getValue().hasTag(tagName.get(), tagValue.orElse(null))) {
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
