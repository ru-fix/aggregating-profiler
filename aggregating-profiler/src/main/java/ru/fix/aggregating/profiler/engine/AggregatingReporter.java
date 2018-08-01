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

    private final Map<String, SharedCounters> sharedCounters = new ConcurrentHashMap<>();

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

    public void updateCounters(String profiledCallName, Consumer<SharedCounters> updateAction) {
        readLock.lock();
        try {
            updateAction.accept(
                    sharedCounters.computeIfAbsent(profiledCallName, key ->
                            new SharedCounters(numberOfActiveCallsToTrackAndKeepBetweenReports)
                    ));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ProfilerReport buildReportAndReset() {
        return buildReportAndReset(Optional.empty());
    }

    @Override
    public ProfilerReport buildReportAndReset(List<Pattern> patterns) {
        return buildReportAndReset(Optional.ofNullable(patterns));
    }

    private ProfilerReport buildReportAndReset(Optional<List<Pattern>> patterns) {
        long timestamp = System.currentTimeMillis();
        long spentTime = timestamp - lastReportTimestamp.getAndSet(timestamp);

        Map<String, Long> indicators = profiler.getIndicators()
                .entrySet()
                .stream()
                .filter(entry -> !patterns.isPresent() || patterns.get()
                        .stream()
                        .anyMatch(p -> p.matcher(entry.getKey()).matches()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> {
                            try {
                                return e.getValue().get();
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                            }
                            return null;
                        }));

        List<ProfiledCallReport> collect = new ArrayList<>();

        writeLock.lock();
        try {
            for (Iterator<Map.Entry<String, SharedCounters>> iterator = sharedCounters.entrySet().iterator();
                 iterator.hasNext(); ) {
                Map.Entry<String, SharedCounters> entry = iterator.next();
                if (patterns.isPresent() && !patterns.get()
                        .stream()
                        .anyMatch(p -> p.matcher(entry.getKey()).matches())) {
                    continue;
                }
                ProfiledCallReport counterReport = buildReportAndReset(entry.getKey(), entry.getValue(), spentTime);

                // skip and remove empty counter
                if (counterReport.getCallsCountSum() == 0 && counterReport.getActiveCallsCountMax() == 0) {
                    iterator.remove();
                    continue;
                }

                collect.add(counterReport);
            }
        } finally {
            writeLock.unlock();
        }

        collect.sort(Comparator.comparing(ProfiledCallReport::getName));

        return new ProfilerReport(indicators, collect);
    }

    private ProfiledCallReport buildReportAndReset(String name, SharedCounters counters, long elapsed) {
        long callsCount = counters.getCallsCount().sumThenReset();
        long startedCallsCount = counters.getStartedCallsCount().sumThenReset();
        long sumStartStopLatency = counters.getSumStartStopLatency().sumThenReset();

        if (callsCount == 0) {
            cleanCounters(counters);
            return new ProfiledCallReport(name)
                    .setStartedCallsCountSum(startedCallsCount)
                    .setActiveCallsCountMax(counters.getActiveCallsCounter().sum())
                    .setActiveCallsLatencyMax(activeCallsMaxLatencyAndResetActiveCalls(counters));
        }

        long payloadTotal = counters.getPayloadSum().sumThenReset();

        return new ProfiledCallReport(name)
                .setLatencyMin(counters.getLatencyMin().getAndSet(Long.MAX_VALUE))
                .setLatencyMax(counters.getLatencyMax().getAndSet(0))
                .setLatencyAvg(sumStartStopLatency / callsCount)

                .setCallsThroughputAvg(elapsed != 0 ? callsCount * 1000 / elapsed : 0)

                .setCallsCountSum(callsCount)
                .setStartedCallsCountSum(startedCallsCount)

                .setPayloadMin(counters.getPayloadMin().getAndSet(Long.MAX_VALUE))
                .setPayloadMax(counters.getPayloadMax().getAndSet(0))
                .setPayloadSum(payloadTotal)
                .setPayloadAvg(payloadTotal / callsCount)
                .setPayloadThroughputAvg(elapsed != 0 ? payloadTotal * 1000 / elapsed : 0)

                .setReportingTimeAvg(elapsed)

                .setThroughputPerSecondMax(counters.getMaxThroughput().getMaxAndReset())
                .setPayloadThroughputPerSecondMax(counters.getMaxPayloadThroughput().getMaxAndReset())

                .setActiveCallsCountMax(counters.getActiveCallsCounter().sum())
                .setActiveCallsLatencyMax(activeCallsMaxLatencyAndResetActiveCalls(counters));
    }

    private long activeCallsMaxLatencyAndResetActiveCalls(SharedCounters counters) {
        Optional<AggregatingCall> longestCall = resetActiveCallsAndGetLongest(counters);
        return longestCall
                .map(AggregatingCall::timeFromCallStart)
                .orElse(0L);

    }

    private Optional<AggregatingCall> resetActiveCallsAndGetLongest(SharedCounters counters) {
        if (numberOfActiveCallsToTrackAndKeepBetweenReports.get() == 0) {
            if(!counters.getActiveCalls().isEmpty()) {
                counters.getActiveCalls().reset();
            }
            return Optional.empty();
        }

        AggregatingCall[] longest = new AggregatingCall[1];

        Set<AggregatingCall> top = new HashSet<>();
        counters.getActiveCalls()
                .stream()
                .sorted(Comparator.comparingLong(AggregatingCall::timeFromCallStart).reversed())
                .limit(numberOfActiveCallsToTrackAndKeepBetweenReports.get())
                .forEachOrdered(call -> {
                    if (top.isEmpty()) {
                        longest[0] = call;
                    }
                    top.add(call);
                });

        Iterator<AggregatingCall> calls = counters.getActiveCalls().iterator();
        while (calls.hasNext()) {
            AggregatingCall call = calls.next();
            if (!top.contains(call)) {
                calls.remove();
            }
        }

        return Optional.ofNullable(longest[0]);
    }

    private static void cleanCounters(SharedCounters counters) {
        counters.getCallsCount().reset();

        counters.getLatencyMax().set(0);
        counters.getLatencyMin().set(Long.MAX_VALUE);
        counters.getSumStartStopLatency().reset();

        counters.getPayloadSum().reset();
        counters.getPayloadMax().set(0);
        counters.getPayloadMin().set(Long.MAX_VALUE);
        counters.getMaxThroughput().reset();
        counters.getMaxPayloadThroughput().reset();
    }

    @Override
    public void close() {
        closingCallback.closed();
    }
}
