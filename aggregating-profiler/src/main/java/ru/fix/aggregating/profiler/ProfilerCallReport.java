package ru.fix.aggregating.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public class ProfilerCallReport {

    final String name;

    long latencyMin;
    long latencyMax;
    long latencyAvg;

    long callsThroughputAvg;
    long callsCountSum;
    long startedCallsCountSum;

    long reportingTimeAvg;

    long payloadMin;
    long payloadMax;
    long payloadAvg;
    long payloadSum;
    long payloadThroughputAvg;

    long throughputPerSecondMax;
    long payloadThroughputPerSecondMax;

    long activeCallsCountMax;
    long activeCallsLatencyMax;

    public ProfilerCallReport(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s: LatMin: %d, LatMax: %d, LatAvg: %d," +
                        " CallsCnt: %d, CallsThrpt: %d, StCallsCnt: %d, RepTime: %d," +
                        " PldMin %d, PldMax %d, PldAvg %d," +
                        " PldTot: %d, PldThrpt: %d," +
                        " MaxThrpt: %d, MaxPldThrpt: %d," +
                        " ActCallsCnt: %d, ActCallsLatMax: %d",
                getName(),
                latencyMin,
                latencyMax,
                latencyAvg,

                callsCountSum,
                callsThroughputAvg,
                startedCallsCountSum,
                reportingTimeAvg,

                payloadMin,
                payloadMax,
                payloadAvg,

                payloadSum,
                payloadThroughputAvg,

                throughputPerSecondMax,
                payloadThroughputPerSecondMax,

                activeCallsCountMax,
                activeCallsLatencyMax);
    }

    public String getName() {
        return name;
    }

    public long getLatencyMin() {
        return latencyMin;
    }

    public long getLatencyMax() {
        return latencyMax;
    }

    public long getLatencyAvg() {
        return latencyAvg;
    }


    public long getReportingTimeAvg() {
        return reportingTimeAvg;
    }

    public ProfilerCallReport setLatencyMin(long latencyMin) {
        this.latencyMin = latencyMin;
        return this;
    }

    public ProfilerCallReport setLatencyMax(long latencyMax) {
        this.latencyMax = latencyMax;
        return this;
    }

    public ProfilerCallReport setLatencyAvg(long latencyAvg) {
        this.latencyAvg = latencyAvg;
        return this;
    }


    public ProfilerCallReport setReportingTimeAvg(long reportingTimeAvg) {
        this.reportingTimeAvg = reportingTimeAvg;
        return this;
    }

    public long getPayloadMin() {
        return payloadMin;
    }

    public ProfilerCallReport setPayloadMin(long payloadMin) {
        this.payloadMin = payloadMin;
        return this;
    }

    public long getPayloadMax() {
        return payloadMax;
    }

    public ProfilerCallReport setPayloadMax(long payloadMax) {
        this.payloadMax = payloadMax;
        return this;
    }

    public long getPayloadAvg() {
        return payloadAvg;
    }

    public ProfilerCallReport setPayloadAvg(long payloadAvg) {
        this.payloadAvg = payloadAvg;
        return this;
    }

    public long getPayloadSum() {
        return payloadSum;
    }

    public ProfilerCallReport setPayloadSum(long payloadSum) {
        this.payloadSum = payloadSum;
        return this;
    }

    public long getCallsCountSum() {
        return callsCountSum;
    }

    public ProfilerCallReport setCallsCountSum(long callsCountSum) {
        this.callsCountSum = callsCountSum;
        return this;
    }

    public long getStartedCallsCountSum() {
        return startedCallsCountSum;
    }

    public ProfilerCallReport setStartedCallsCountSum(long startedCallsCountSum) {
        this.startedCallsCountSum = startedCallsCountSum;
        return this;
    }

    public long getCallsThroughputAvg() {
        return callsThroughputAvg;
    }

    public ProfilerCallReport setCallsThroughputAvg(long callsThroughputAvg) {
        this.callsThroughputAvg = callsThroughputAvg;
        return this;
    }

    public long getPayloadThroughputAvg() {
        return payloadThroughputAvg;
    }

    public ProfilerCallReport setPayloadThroughputAvg(long payloadThroughputAvg) {
        this.payloadThroughputAvg = payloadThroughputAvg;
        return this;
    }

    public long getThroughputPerSecondMax() {
        return throughputPerSecondMax;
    }

    public ProfilerCallReport setThroughputPerSecondMax(long throughputPerSecondMax) {
        this.throughputPerSecondMax = throughputPerSecondMax;
        return this;
    }

    public long getPayloadThroughputPerSecondMax() {
        return payloadThroughputPerSecondMax;
    }

    public ProfilerCallReport setPayloadThroughputPerSecondMax(long payloadThroughputPerSecondMax) {
        this.payloadThroughputPerSecondMax = payloadThroughputPerSecondMax;
        return this;
    }

    public long getActiveCallsCountMax() {
        return activeCallsCountMax;
    }

    public ProfilerCallReport setActiveCallsCountMax(long activeCallsCountMax) {
        this.activeCallsCountMax = activeCallsCountMax;
        return this;
    }

    public long getActiveCallsLatencyMax() {
        return activeCallsLatencyMax;
    }

    public ProfilerCallReport setActiveCallsLatencyMax(long activeCallsLatencyMax) {
        this.activeCallsLatencyMax = activeCallsLatencyMax;
        return this;
    }
}
