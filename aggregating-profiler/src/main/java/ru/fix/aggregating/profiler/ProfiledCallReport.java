package ru.fix.aggregating.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public class ProfiledCallReport {

    final String name;

    long latencyMin;
    long latencyMax;
    long latencyAvg;

    long callsThroughputAvg;
    long callsCountSum;

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

    public ProfiledCallReport(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s: LatMin: %d, LatMax: %d, LatAvg: %d," +
                        " CallsCnt: %d, CallsThrpt: %d, RepTime: %d," +
                        " PldMin %d, PldMax %d, PldAvg %d," +
                        " PldSum: %d, PldThrpt: %d," +
                        " MaxThrpt: %d, MaxPldThrpt: %d," +
                        " ActCallsCnt: %d, ActCallsLatMax: %d",
                getName(),
                latencyMin,
                latencyMax,
                latencyAvg,

                callsCountSum,
                callsThroughputAvg,
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

    public ProfiledCallReport setLatencyMin(long latencyMin) {
        this.latencyMin = latencyMin;
        return this;
    }

    public ProfiledCallReport setLatencyMax(long latencyMax) {
        this.latencyMax = latencyMax;
        return this;
    }

    public ProfiledCallReport setLatencyAvg(long latencyAvg) {
        this.latencyAvg = latencyAvg;
        return this;
    }


    public ProfiledCallReport setReportingTimeAvg(long reportingTimeAvg) {
        this.reportingTimeAvg = reportingTimeAvg;
        return this;
    }

    public long getPayloadMin() {
        return payloadMin;
    }

    public ProfiledCallReport setPayloadMin(long payloadMin) {
        this.payloadMin = payloadMin;
        return this;
    }

    public long getPayloadMax() {
        return payloadMax;
    }

    public ProfiledCallReport setPayloadMax(long payloadMax) {
        this.payloadMax = payloadMax;
        return this;
    }

    public long getPayloadAvg() {
        return payloadAvg;
    }

    public ProfiledCallReport setPayloadAvg(long payloadAvg) {
        this.payloadAvg = payloadAvg;
        return this;
    }

    public long getPayloadSum() {
        return payloadSum;
    }

    public ProfiledCallReport setPayloadSum(long payloadSum) {
        this.payloadSum = payloadSum;
        return this;
    }

    public long getCallsCountSum() {
        return callsCountSum;
    }

    public ProfiledCallReport setCallsCountSum(long callsCountSum) {
        this.callsCountSum = callsCountSum;
        return this;
    }

    /**
     * Average rate of profiledCall milli invocation per second
     * (123 means that there was 0.123 invocation per second)
     */
    public long getCallsThroughputAvg() {
        return callsThroughputAvg;
    }

    public ProfiledCallReport setCallsThroughputAvg(long callsThroughputAvg) {
        this.callsThroughputAvg = callsThroughputAvg;
        return this;
    }

    /**
     * Average payload rate milli invocation per second
     * (123 means that there was 0.123 invocation per second)
     */
    public long getPayloadThroughputAvg() {
        return payloadThroughputAvg;
    }

    public ProfiledCallReport setPayloadThroughputAvg(long payloadThroughputAvg) {
        this.payloadThroughputAvg = payloadThroughputAvg;
        return this;
    }

    public long getThroughputPerSecondMax() {
        return throughputPerSecondMax;
    }

    public ProfiledCallReport setThroughputPerSecondMax(long throughputPerSecondMax) {
        this.throughputPerSecondMax = throughputPerSecondMax;
        return this;
    }

    public long getPayloadThroughputPerSecondMax() {
        return payloadThroughputPerSecondMax;
    }

    public ProfiledCallReport setPayloadThroughputPerSecondMax(long payloadThroughputPerSecondMax) {
        this.payloadThroughputPerSecondMax = payloadThroughputPerSecondMax;
        return this;
    }

    public long getActiveCallsCountMax() {
        return activeCallsCountMax;
    }

    public ProfiledCallReport setActiveCallsCountMax(long activeCallsCountMax) {
        this.activeCallsCountMax = activeCallsCountMax;
        return this;
    }

    public long getActiveCallsLatencyMax() {
        return activeCallsLatencyMax;
    }

    public ProfiledCallReport setActiveCallsLatencyMax(long activeCallsLatencyMax) {
        this.activeCallsLatencyMax = activeCallsLatencyMax;
        return this;
    }
}
