package ru.fix.commons.profiler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;
import ru.fix.commons.profiler.ProfilerReporter;
import ru.fix.commons.profiler.MetricsGroupTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class ProfilerReporterImpl implements ProfilerReporter {
    private static final Logger log = LoggerFactory.getLogger(ProfilerReporterImpl.class);

    private final Map<String, SharedCounters> sharedCounters = new ConcurrentHashMap<>();
    private final Map<String, Set<Pattern>> groupSeparator = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    // update counters lock
    private final Lock readLock = readWriteLock.readLock();
    // create report and then reset counters lock
    private final Lock writeLock = readWriteLock.writeLock();

    private final SimpleProfiler profiler;

    private final AtomicLong lastReportTimestamp;

    private final AtomicBoolean enableActiveCallsMaxLatency;
    private final AtomicInteger numberOfActiveCallsToKeepBetweenReports;

    public ProfilerReporterImpl(SimpleProfiler profiler) {
        this(profiler, false, 20);
    }

    public ProfilerReporterImpl(SimpleProfiler profiler,
                                boolean enableActiveCallsMaxLatency,
                                int numberOfActiveCallsToKeepBetweenReports) {
        this.profiler = profiler;
        this.enableActiveCallsMaxLatency = new AtomicBoolean(enableActiveCallsMaxLatency);
        this.numberOfActiveCallsToKeepBetweenReports = new AtomicInteger(numberOfActiveCallsToKeepBetweenReports);
        lastReportTimestamp = new AtomicLong(System.currentTimeMillis());

        this.profiler.registerReporter(this);
    }

    @Override
    public void setGroupsSeparator(Map<String, Set<Pattern>> groupSeparator) {
        this.groupSeparator.clear();
        this.groupSeparator.putAll(groupSeparator);
        writeLock.lock();
        this.sharedCounters.forEach((k, v) -> v.getGroupTag().evalGroupTag(k, groupSeparator));
        writeLock.unlock();
    }

    @Override
    public boolean setEnableActiveCallsMaxLatency(boolean enable) {
        boolean prevValue = this.enableActiveCallsMaxLatency.getAndSet(enable);

        sharedCounters.values().forEach((counters) ->
                counters.setRecordActiveCalls(enable)
        );

        return prevValue;
    }

    @Override
    public int setNumberOfActiveCallsToKeepBetweenReports(int number) {
        return this.numberOfActiveCallsToKeepBetweenReports.getAndSet(number);
    }

    public void applyToSharedCounters(String profiledCallName, Consumer<SharedCounters> consumer) {
        readLock.lock();
        try {
            consumer.accept(
                sharedCounters.computeIfAbsent(
                    profiledCallName,
                    key -> {
                        SharedCounters sc =  new SharedCounters(enableActiveCallsMaxLatency.get());
                        sc.getGroupTag().evalGroupTag(profiledCallName, groupSeparator);
                        return sc;
                    }));
        } finally {
            readLock.unlock();
        }
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

        List<ProfilerCallReport> collect = new ArrayList<>();

        writeLock.lock();
        try {
            for (Iterator<Map.Entry<String, SharedCounters>> iterator = sharedCounters.entrySet().iterator();
                 iterator.hasNext(); ) {
                Map.Entry<String, SharedCounters> entry = iterator.next();
                if (tag.isPresent() && ! entry.getValue().getGroupTag().getTagValue().equals(tag.orElse(null))) {
                    continue;
                }
                ProfilerCallReport counterReport = buildReportAndReset(entry.getKey(), entry.getValue(), spentTime);

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

        collect.sort(Comparator.comparing(ProfilerCallReport::getName));

        return new ProfilerReport(indicators, collect);
    }

    private ProfilerCallReport buildReportAndReset(String name, SharedCounters counters, long elapsed) {
        long callsCount = counters.getCallsCount().sumThenReset();
        long startedCallsCount = counters.getStartedCallsCount().sumThenReset();
        long sumStartStopLatency = counters.getSumStartStopLatency().sumThenReset();

        if (callsCount == 0) {
            cleanCounters(counters);
            return new ProfilerCallReport(name)
                    .setStartedCallsCountSum(startedCallsCount)
                    .setActiveCallsCountMax(counters.getActiveCallsCounter().sum())
                    .setActiveCallsLatencyMax(activeCallsMaxLatencyAndResetActiveCalls(counters));
        }

        long payloadTotal = counters.getPayloadSum().sumThenReset();

        return new ProfilerCallReport(name)
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
        Optional<ProfiledCallImpl> longestCall = resetActiveCallsAndGetLongest(counters);
        return longestCall
                .map(ProfiledCallImpl::timeFromCallStartInMs)
                .orElse(0L);

    }

    private Optional<ProfiledCallImpl> resetActiveCallsAndGetLongest(SharedCounters counters) {
        if (!enableActiveCallsMaxLatency.get() && !counters.getActiveCalls().isEmpty()) {
            counters.getActiveCalls().reset();
            return Optional.empty();
        }

        ProfiledCallImpl[] longest = new ProfiledCallImpl[1];

        Set<ProfiledCallImpl> top = new HashSet<>();
        counters.getActiveCalls()
                .stream()
                .sorted(Comparator.comparingLong(ProfiledCallImpl::startTime))
                .limit(numberOfActiveCallsToKeepBetweenReports.get())
                .forEachOrdered(call -> {
                    if (top.isEmpty()) {
                        longest[0] = call;
                    }
                    top.add(call);
                });

        Iterator<ProfiledCallImpl> calls = counters.getActiveCalls().iterator();
        while (calls.hasNext()) {
            ProfiledCallImpl call = calls.next();
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
        profiler.unregisterReporter(this);
    }
}
