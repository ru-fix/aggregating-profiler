package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.ProfiledCallReport;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
public class CallAggregate {

    private final String callName;

    private final LongAdder callsCountSum = new LongAdder();
    private final LongAdder latencySum = new LongAdder();

    private final LongAccumulator latencyMin = new LongAccumulator(Math::min, Long.MAX_VALUE);
    private final LongAccumulator latencyMax = new LongAccumulator(Math::max, 0L);


    private final LongAdder payloadSum = new LongAdder();
    private final LongAccumulator payloadMin = new LongAccumulator(Math::min, Long.MAX_VALUE);
    private final LongAccumulator payloadMax = new LongAccumulator(Math::max, 0L);

    private final MaxThroughputPerSecondAccumulator maxThroughputPerSecond = new MaxThroughputPerSecondAccumulator();
    private final MaxThroughputPerSecondAccumulator maxPayloadThroughputPerSecond = new MaxThroughputPerSecondAccumulator();

    private final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports;

    private final LongAdder activeCallsSum = new LongAdder();
    private final Set<AggregatingCall> activeCalls = ConcurrentHashMap.newKeySet();

    public CallAggregate(
            String callName,
            AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports
            ) {
        this.callName = callName;
        this.numberOfActiveCallsToTrackAndKeepBetweenReports = numberOfActiveCallsToTrackAndKeepBetweenReports;

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
                .sorted(Comparator.comparingLong(AggregatingCall::timeFromCallStart).reversed())
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
        long callsCount = callsCountSum.sumThenReset();

        ProfiledCallReport report = new ProfiledCallReport(callName)
                .setActiveCallsCountMax(activeCallsSum.sum())
                .setActiveCallsLatencyMax(activeCallsMaxLatencyAndResetActiveCalls())
                .setReportingTimeAvg(elapsed);

        if (callsCount == 0) {
            return report;
        }

        long payloadTotal = payloadSum.sumThenReset();

        return report
                .setLatencyMin(latencyMin.getThenReset())
                .setLatencyMax(latencyMax.getThenReset())
                .setLatencyAvg(latencySum.sumThenReset() / callsCount)

                .setCallsThroughputAvg(elapsed != 0 ? callsCount * 1000_000 / elapsed : 0)

                .setCallsCountSum(callsCount)

                .setPayloadMin(payloadMin.getThenReset())
                .setPayloadMax(payloadMax.getThenReset())
                .setPayloadSum(payloadTotal)
                .setPayloadAvg(payloadTotal / callsCount)

                .setPayloadThroughputAvg(elapsed != 0 ? payloadTotal * 1000_000 / elapsed : 0)

                .setThroughputPerSecondMax(maxThroughputPerSecond.getAndReset())
                .setPayloadThroughputPerSecondMax(maxPayloadThroughputPerSecond.getAndReset());
    }

    private long activeCallsMaxLatencyAndResetActiveCalls() {
        Optional<AggregatingCall> longestCall = resetActiveCallsAndGetLongest();
        return longestCall
                .map(AggregatingCall::timeFromCallStart)
                .orElse(0L);

    }
}
