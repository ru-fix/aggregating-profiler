package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.ProfiledCallReport;
import ru.fix.aggregating.profiler.Tagged;

import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
public class CallAggregate implements Tagged {

    final Identity callIdentity;

    final LongAdder callsCountSum = new LongAdder();
    final LongAdder latencySum = new LongAdder();

    final LongAccumulator latencyMin = new LongAccumulator(Math::min, Long.MAX_VALUE);
    final LongAccumulator latencyMax = new LongAccumulator(Math::max, 0L);


    final LongAdder payloadSum = new LongAdder();
    final LongAccumulator payloadMin = new LongAccumulator(Math::min, Long.MAX_VALUE);
    final LongAccumulator payloadMax = new LongAccumulator(Math::max, 0L);

    final MaxThroughputPerSecondAccumulator maxThroughputPerSecond = new MaxThroughputPerSecondAccumulator();
    final MaxThroughputPerSecondAccumulator maxPayloadThroughputPerSecond = new MaxThroughputPerSecondAccumulator();

    final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports;

    final LongAdder activeCallsSum = new LongAdder();
    final Set<AggregatingCall> activeCalls = ConcurrentHashMap.newKeySet();
    final Map<String, String> tags = new ConcurrentHashMap<>();

    public CallAggregate(
            Identity callIdentity,
            AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports
            ) {
        this.callIdentity = callIdentity;
        this.numberOfActiveCallsToTrackAndKeepBetweenReports = numberOfActiveCallsToTrackAndKeepBetweenReports;

    }

    @Override
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    @Override
    public void setTag(String name, String value) {
        this.tags.put(name, value);
    }

    @Override
    public boolean hasTag(String tagName, String tagValue) {
        return tags.containsKey(tagName) && tags.get(tagName).equals(tagValue);
    }
    
    /**
     * @param currentTimestamp
     * @param latency Ignored in case of -1
     * @param payload Ignored in case of -1
     */
    public void call(long currentTimestamp, long latency, long payload) {

        callsCountSum.increment();

        latencyMin.accumulate(latency);
        if(latency > 0) {
            latencySum.add(latency);
            latencyMax.accumulate(latency);
        }

        payloadMin.accumulate(payload);
        if(payload > 0) {
            payloadSum.add(payload);
            payloadMax.accumulate(payload);
        }


        maxThroughputPerSecond.call(currentTimestamp, 1);

        if (payload > 0) {
            maxPayloadThroughputPerSecond.call(currentTimestamp, payload);
        }

    }

    public void start(AggregatingCall profiledCall){
        activeCallsSum.increment();
        if(numberOfActiveCallsToTrackAndKeepBetweenReports.get() > 0) {
            activeCalls.add(profiledCall);
        }

    }

    public void stop(AggregatingCall profiledCall, long currentTimestamp, long latency, long payload) {
        callsCountSum.increment();

        latencyMin.accumulate(latency);
        if (latency > 0) {
            latencySum.add(latency);
            latencyMax.accumulate(latency);
        }

        payloadMin.accumulate(payload);
        if (payload > 0) {
            payloadMax.accumulate(payload);
            payloadSum.add(payload);
        }


        maxThroughputPerSecond.call(currentTimestamp, 1);
        if (payload > 0) {
            maxPayloadThroughputPerSecond.call(currentTimestamp, payload);
        }


        activeCalls.remove(profiledCall);
        activeCallsSum.decrement();
    }

    public void close(AggregatingCall call){
        activeCalls.remove(call);
        activeCallsSum.decrement();
    }

    public Optional<AggregatingCall> resetActiveCallsAndGetLongest() {
        if (numberOfActiveCallsToTrackAndKeepBetweenReports.get() == 0) {
            if(!activeCalls.isEmpty()) {
                activeCalls.clear();
                activeCallsSum.reset();
            }
            return Optional.empty();
        }

        AggregatingCall[] longest = new AggregatingCall[1];

        Set<AggregatingCall> top = new HashSet<>();
        activeCalls
                .stream()
                .sorted(Comparator.comparingLong(AggregatingCall::startNanoTime))
                .limit(numberOfActiveCallsToTrackAndKeepBetweenReports.get())
                .forEachOrdered(call -> {
                    if (top.isEmpty()) {
                        longest[0] = call;
                    }
                    top.add(call);
                });

        activeCalls.removeIf(call -> !top.contains(call));

        return Optional.ofNullable(longest[0]);
    }

    public ProfiledCallReport buildReportAndReset(long elapsed) {
        long callsCount = LongAdderDrainer.drain(callsCountSum);

        ProfiledCallReport report = new ProfiledCallReport(callName)
                .setActiveCallsCountMax(activeCallsSum.sum())
                .setActiveCallsLatencyMax(activeCallsMaxLatencyAndResetActiveCalls())
                .setReportingTimeAvg(elapsed);

        if (callsCount == 0) {
            return report;
        }

        long payloadTotal = LongAdderDrainer.drain(payloadSum);

        return report
                .setLatencyMin(latencyMin.getThenReset())
                .setLatencyMax(latencyMax.getThenReset())
                .setLatencyAvg(LongAdderDrainer.drain(latencySum) / callsCount)

                .setCallsThroughputAvg(elapsed != 0 ? ((double)callsCount * 1000) / elapsed : 0)

                .setCallsCountSum(callsCount)

                .setPayloadMin(payloadMin.getThenReset())
                .setPayloadMax(payloadMax.getThenReset())
                .setPayloadSum(payloadTotal)
                .setPayloadAvg(payloadTotal / callsCount)

                .setPayloadThroughputAvg(elapsed != 0 ? ((double)payloadTotal * 1000) / elapsed : 0)

                .setThroughputPerSecondMax(maxThroughputPerSecond.getAndReset(System.currentTimeMillis()))
                .setPayloadThroughputPerSecondMax(maxPayloadThroughputPerSecond.getAndReset(System.currentTimeMillis()));
    }

    private long activeCallsMaxLatencyAndResetActiveCalls() {
        Optional<AggregatingCall> longestCall = resetActiveCallsAndGetLongest();
        return longestCall
                .map(AggregatingCall::timeFromCallStart)
                .orElse(0L);

    }
}
