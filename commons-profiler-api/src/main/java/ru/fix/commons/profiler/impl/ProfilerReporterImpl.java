package ru.fix.commons.profiler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;
import ru.fix.commons.profiler.ProfilerReporter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private final AtomicLong lastReportTimestamp = new AtomicLong(System.currentTimeMillis());


    public ProfilerReporterImpl(SimpleProfiler profiler) {
        this.profiler = profiler;
        this.profiler.registerReporter(this);
    }

    public void applyToSharedCounters(String profiledCallName, Consumer<SharedCounters> consumer) {
        readLock.lock();
        try {
            consumer.accept(sharedCounters.computeIfAbsent(profiledCallName, key -> new SharedCounters()));
        } finally {
            readLock.unlock();
        }
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
            cleanCounters(counters);
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

                .setReportingTime(elapsed);
    }

    private void cleanCounters(SharedCounters counters) {
        counters.getCallsCount().reset();

        counters.getLatencyMax().set(0);
        counters.getLatencyMin().set(Long.MAX_VALUE);
        counters.getSumStartStopLatency().reset();

        counters.getPayloadSum().reset();
        counters.getPayloadMax().set(0);
        counters.getPayloadMin().set(Long.MAX_VALUE);
    }

    @Override
    public void close() throws Exception {
        profiler.unregisterReporter(this);
    }
}
