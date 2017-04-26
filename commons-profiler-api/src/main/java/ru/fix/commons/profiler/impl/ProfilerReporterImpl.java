package ru.fix.commons.profiler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.ProfilerCallReport;
import ru.fix.commons.profiler.ProfilerReport;
import ru.fix.commons.profiler.ProfilerReporter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

class ProfilerReporterImpl implements ProfilerReporter {
    private static final Logger log = LoggerFactory.getLogger(ProfilerReporterImpl.class);

    private final Map<String, SharedCounters> sharedCounters = new ConcurrentHashMap<>();

    private final SimpleProfiler profiler;

    private AtomicLong lastReportTimestamp = new AtomicLong();


    public ProfilerReporterImpl(SimpleProfiler profiler) {
        this.profiler = profiler;
        this.profiler.registerReporter(this);
    }

    public SharedCounters getSharedCounters(String profiledCallName) {
        return sharedCounters.computeIfAbsent(profiledCallName, key -> new SharedCounters());
    }

    @Override
    public ProfilerReport buildReportAndReset() {
        long timestamp = System.currentTimeMillis();
        long spentTime = timestamp - lastReportTimestamp.get();
        lastReportTimestamp.set(timestamp);

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

        report.setProfilerCallReports(sharedCounters.keySet()
                .stream()
                .sorted()
                .map(name -> buildReportAndReset(name, spentTime))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));
        return report;
    }


    private Optional<ProfilerCallReport> buildReportAndReset(String name, long elapsed) {
        SharedCounters counters = sharedCounters.get(name);

        long callsCount = counters.getCallsCount().sumThenReset();
        long sumStartStopLatency = counters.getSumStartStopLatency().sumThenReset();

        if (callsCount == 0) {
            cleanCounters(counters);
            return Optional.empty();
        }

        long payloadTotal = counters.getPayloadSum().sumThenReset();

        return Optional.of(new ProfilerCallReport(name)
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

                .setReportingTime(elapsed));
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
