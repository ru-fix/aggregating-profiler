package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.Identity;
import ru.fix.aggregating.profiler.ProfiledCallReport;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
public class CallAggregate implements AutoLabelStickerable {

    final Identity callIdentity;

    final LongAdder startSumAdder = new LongAdder();

    final LongAdder stopSumAdder = new LongAdder();
    final LongAdder latencySum = new LongAdder();

    final LongAccumulator latencyMin = new LongAccumulator(Math::min, Long.MAX_VALUE);
    final LongAccumulator latencyMax = new LongAccumulator(Math::max, 0L);


    final LongAdder payloadSumAdder = new LongAdder();
    final LongAccumulator payloadMin = new LongAccumulator(Math::min, Long.MAX_VALUE);
    final LongAccumulator payloadMax = new LongAccumulator(Math::max, 0L);

    final MaxThroughputPerSecondAccumulator startMaxThroughputPerSecondAcc = new MaxThroughputPerSecondAccumulator();
    final MaxThroughputPerSecondAccumulator stopMaxThroughputPerSecondAcc = new MaxThroughputPerSecondAccumulator();
    final MaxThroughputPerSecondAccumulator payloadMaxThroughputPerSecondAcc = new MaxThroughputPerSecondAccumulator();

    final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports;

    final LongAdder activeCallsCountSumAdder = new LongAdder();
    final Set<AggregatingCall> activeCalls = ConcurrentHashMap.newKeySet();

    final Map<String, String> autoLabels = new ConcurrentHashMap<>();

    public CallAggregate(
            Identity callIdentity,
            AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports
    ) {
        this.callIdentity = callIdentity;
        this.numberOfActiveCallsToTrackAndKeepBetweenReports = numberOfActiveCallsToTrackAndKeepBetweenReports;

    }

    @Override
    public void setAutoLabel(String name, String value) {
        this.autoLabels.put(name, value);
    }

    @Override
    public Map<String, String> getAutoLabels() {
        return this.autoLabels;
    }


    public void call(long currentTimestamp, long latency, long payload) {
        startSumAdder.increment();
        stopSumAdder.increment();

        latencyMin.accumulate(latency);
        if (latency > 0) {
            latencySum.add(latency);
            latencyMax.accumulate(latency);
        }

        payloadMin.accumulate(payload);
        if (payload > 0) {
            payloadSumAdder.add(payload);
            payloadMax.accumulate(payload);
        }

        startMaxThroughputPerSecondAcc.call(currentTimestamp, 1);
        stopMaxThroughputPerSecondAcc.call(currentTimestamp, 1);

        if (payload > 0) {
            payloadMaxThroughputPerSecondAcc.call(currentTimestamp, payload);
        }

    }

    public void start(AggregatingCall profiledCall, long currentTimestamp) {
        startSumAdder.increment();
        startMaxThroughputPerSecondAcc.call(currentTimestamp, 1);

        activeCallsCountSumAdder.increment();
        if (numberOfActiveCallsToTrackAndKeepBetweenReports.get() > 0) {
            activeCalls.add(profiledCall);
        }

    }

    public void stop(AggregatingCall profiledCall, long currentTimestamp, long latency, long payload) {
        stopSumAdder.increment();

        latencyMin.accumulate(latency);
        if (latency > 0) {
            latencySum.add(latency);
            latencyMax.accumulate(latency);
        }

        payloadMin.accumulate(payload);
        if (payload > 0) {
            payloadMax.accumulate(payload);
            payloadSumAdder.add(payload);
        }


        stopMaxThroughputPerSecondAcc.call(currentTimestamp, 1);
        if (payload > 0) {
            payloadMaxThroughputPerSecondAcc.call(currentTimestamp, payload);
        }


        activeCalls.remove(profiledCall);
        activeCallsCountSumAdder.decrement();
    }

    public void close(AggregatingCall call) {
        activeCalls.remove(call);
        activeCallsCountSumAdder.decrement();
    }

    public Optional<AggregatingCall> resetActiveCallsAndGetLongest() {
        if (numberOfActiveCallsToTrackAndKeepBetweenReports.get() == 0) {
            if (!activeCalls.isEmpty()) {
                activeCalls.clear();
                activeCallsCountSumAdder.reset();
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
        long startSum = LongAdderDrainer.drain(startSumAdder);
        long stopSum = LongAdderDrainer.drain(stopSumAdder);

        ProfiledCallReport report = new ProfiledCallReport(this.callIdentity)
                .setReportingTimeAvg(elapsed)

                .setActiveCallsCountMax(activeCallsCountSumAdder.sum())
                .setActiveCallsLatencyMax(activeCallsMaxLatencyAndResetActiveCalls());

        if (stopSum == 0) {
            return report;
        }

        long payloadSum = LongAdderDrainer.drain(payloadSumAdder);

        return report
                .setStartSum(startSum)
                .setStartThroughputPerSecondMax(startMaxThroughputPerSecondAcc.getAndReset(System.currentTimeMillis()))
                .setStartThroughputAvg(elapsed != 0 ? ((double) startSum * 1000) / elapsed : 0)

                .setLatencyMin(latencyMin.getThenReset())
                .setLatencyMax(latencyMax.getThenReset())
                .setLatencyAvg(LongAdderDrainer.drain(latencySum) / stopSum)

                .setPayloadMin(payloadMin.getThenReset())
                .setPayloadMax(payloadMax.getThenReset())
                .setPayloadSum(payloadSum)
                .setPayloadAvg(payloadSum / stopSum)
                .setPayloadThroughputAvg(elapsed != 0 ? ((double) payloadSum * 1000) / elapsed : 0)
                .setPayloadThroughputPerSecondMax(payloadMaxThroughputPerSecondAcc.getAndReset(System.currentTimeMillis()))

                .setStopSum(stopSum)
                .setStopThroughputAvg(elapsed != 0 ? ((double) stopSum * 1000) / elapsed : 0)
                .setStopThroughputPerSecondMax(stopMaxThroughputPerSecondAcc.getAndReset(System.currentTimeMillis()))
                ;
    }

    private long activeCallsMaxLatencyAndResetActiveCalls() {
        Optional<AggregatingCall> longestCall = resetActiveCallsAndGetLongest();
        return longestCall
                .map(AggregatingCall::timeFromCallStart)
                .orElse(0L);

    }
}
