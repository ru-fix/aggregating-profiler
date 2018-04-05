package ru.fix.commons.profiler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;
import ru.fix.commons.profiler.ProfilerReporter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class ProfilerReporterImpl implements ProfilerReporter {
    private static final Logger log = LoggerFactory.getLogger(ProfilerReporterImpl.class);

    private final Map<String, SharedCounters> sharedCounters = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    // update counters lock
    private final Lock readLock = readWriteLock.readLock();
    // create report and then reset counters lock
    private final Lock writeLock = readWriteLock.writeLock();

    private final SimpleProfiler profiler;

    private final AtomicLong lastReportTimestamp;

    private final ConcurrentHashMap<String, AtomicLong> activeCallsCounter = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Set<ProfiledCallImpl>> longestActiveCalls = new ConcurrentHashMap<>();

    private final AtomicBoolean enableActiveCallsMaxLatency;
    private final AtomicInteger numberOfActiveCallsToKeepBetweenReports;

    public ProfilerReporterImpl(SimpleProfiler profiler) {
        this(profiler, false, 20);
    }

    public ProfilerReporterImpl(SimpleProfiler profiler,
                                boolean enableActiveCallsMaxLatency,
                                int numberOfActiveCallsToKeepBetweenReports) {
        this.profiler = profiler;
        this.profiler.registerReporter(this);
        this.enableActiveCallsMaxLatency = new AtomicBoolean(enableActiveCallsMaxLatency);
        this.numberOfActiveCallsToKeepBetweenReports = new AtomicInteger(numberOfActiveCallsToKeepBetweenReports);
        lastReportTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public boolean setEnableActiveCallsMaxLatency(boolean enable) {
        return this.enableActiveCallsMaxLatency.getAndSet(enable);
    }

    @Override
    public int setNumberOfActiveCallsToKeepBetweenReports(int number) {
        return this.numberOfActiveCallsToKeepBetweenReports.getAndSet(number);
    }

    public void applyToSharedCounters(String profiledCallName, Consumer<SharedCounters> consumer) {
        readLock.lock();
        try {
            consumer.accept(sharedCounters.computeIfAbsent(profiledCallName, key -> new SharedCounters()));
        } finally {
            readLock.unlock();
        }
    }

    public void callStarted(ProfiledCallImpl call) {
        activeCallsCounter
                .computeIfAbsent(call.profiledCallName, (callName) -> new AtomicLong())
                .incrementAndGet();

        if (!enableActiveCallsMaxLatency.get()) {
            return;
        }
        this.longestActiveCalls.compute(call.profiledCallName, (name, profiledCalls) -> {
            if (profiledCalls == null) {
                profiledCalls = new HashSet<>();
            }
            profiledCalls.add(call);
            return profiledCalls;
        });
    }

    public void callEnded(ProfiledCallImpl call) {
        activeCallsCounter.get(call.profiledCallName).decrementAndGet();

        if (!enableActiveCallsMaxLatency.get() && this.longestActiveCalls.isEmpty()) {
            return;
        }

        this.longestActiveCalls.compute(call.profiledCallName, (name, profiledCalls) -> {
            //noinspection ConstantConditions
            profiledCalls.remove(call);
            return profiledCalls;
        });
    }

    @Override
    public ProfilerReport buildReportAndReset() {
        long timestamp = System.currentTimeMillis();
        long spentTime = timestamp - lastReportTimestamp.getAndSet(timestamp);

        ProfilerReport report = new ProfilerReport();
        report.setIndicators(profiler.getIndicators()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> {
                            try {
                                return e.getValue().get();
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                            }
                            return null;
                        })));

        List<ProfilerCallReport> collect = new ArrayList<>();

        writeLock.lock();
        try {
            for (Iterator<Map.Entry<String, SharedCounters>> iterator = sharedCounters.entrySet().iterator();
                 iterator.hasNext(); ) {
                Map.Entry<String, SharedCounters> entry = iterator.next();
                ProfilerCallReport counterReport = buildReportAndReset(entry.getKey(), entry.getValue(), spentTime);

                // skip and remove empty counter
                if (counterReport.getCallsCount() == 0) {
                    iterator.remove();
                    continue;
                }

                collect.add(counterReport);
            }
        } finally {
            writeLock.unlock();
        }

        collect.sort(Comparator.comparing(ProfilerCallReport::getName));
        report.setProfilerCallReports(collect);
        return report;
    }

    private ProfilerCallReport buildReportAndReset(String name, SharedCounters counters, long elapsed) {
        long callsCount = counters.getCallsCount().sumThenReset();
        long sumStartStopLatency = counters.getSumStartStopLatency().sumThenReset();

        if (callsCount == 0) {
            cleanCounters(name, counters);
            return new ProfilerCallReport(name);
        }

        long payloadTotal = counters.getPayloadSum().sumThenReset();

        return new ProfilerCallReport(name)
                .setMinLatency(counters.getLatencyMin().getAndSet(Long.MAX_VALUE))
                .setMaxLatency(counters.getLatencyMax().getAndSet(0))
                .setAvgLatency(sumStartStopLatency / callsCount)

                .setCallsThroughput(elapsed != 0 ? callsCount * 1000 / elapsed : 0)

                .setCallsCount(callsCount)

                .setPayloadMin(counters.getPayloadMin().getAndSet(Long.MAX_VALUE))
                .setPayloadMax(counters.getPayloadMax().getAndSet(0))
                .setPayloadTotal(payloadTotal)
                .setPayloadAvg(payloadTotal / callsCount)
                .setPayloadThroughput(elapsed != 0 ? payloadTotal * 1000 / elapsed : 0)

                .setReportingTime(elapsed)

                .setMaxThroughputPerSecond(counters.getMaxThroughput().getMaxAndReset())
                .setMaxPayloadThroughputPerSecond(counters.getMaxPayloadThroughput().getMaxAndReset())

                .setActiveCallsCount(activeCallsCountCount(name))
                .setActiveCallsMaxLatency(activeCallsMaxLatencyAndResetActiveCalls(name));
    }

    private long activeCallsCountCount(String callName) {
        AtomicLong activeCallsCount = activeCallsCounter.get(callName);
        return activeCallsCount != null ? activeCallsCount.get() : 0;
    }

    private long activeCallsMaxLatencyAndResetActiveCalls(String callName) {
        Set<ProfiledCallImpl> activeCalls = this.longestActiveCalls.get(callName);
        if (activeCalls == null) {
            return 0L;
        }

        ProfiledCallImpl longestCall = resetActiveCallsAndGetLongest(callName);
        if (longestCall == null) {
            return 0L;
        }

        return longestCall.timeFromCallStartInMs();
    }

    private ProfiledCallImpl resetActiveCallsAndGetLongest(String callName) {
        ProfiledCallImpl[] longest = new ProfiledCallImpl[1];

        this.longestActiveCalls.compute(callName, (name, calls) -> {
            if (calls == null || calls.isEmpty()) {
                return calls;
            }

            Set<ProfiledCallImpl> top = new HashSet<>(
                    (int) (calls.size() / 0.75f) + 1, 0.75f
            );
            calls
                    .stream()
                    .sorted(Comparator.comparingLong(ProfiledCallImpl::startTime))
                    .limit(numberOfActiveCallsToKeepBetweenReports.get())
                    .forEachOrdered(call -> {
                        if (top.isEmpty()) {
                            longest[0] = call;
                        }
                        top.add(call);
                    });
            return top;
        });
        return longest[0];
    }

    private void cleanCounters(String callName, SharedCounters counters) {
        counters.getCallsCount().reset();

        counters.getLatencyMax().set(0);
        counters.getLatencyMin().set(Long.MAX_VALUE);
        counters.getSumStartStopLatency().reset();

        counters.getPayloadSum().reset();
        counters.getPayloadMax().set(0);
        counters.getPayloadMin().set(Long.MAX_VALUE);
        counters.getMaxThroughput().reset();
        counters.getMaxPayloadThroughput().reset();

        resetActiveCallsAndGetLongest(callName);
    }

    @Override
    public void close() {
        profiler.unregisterReporter(this);
    }
}
