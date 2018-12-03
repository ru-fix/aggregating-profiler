package ru.fix.aggregating.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public class ProfiledCallReport {

    final String name;

    long latencyMin;
    long latencyMax;
    long latencyAvg;

    double callsThroughputAvg;
    long callsCountSum;

    long reportingTimeAvg;

    long payloadMin;
    long payloadMax;
    long payloadAvg;
    long payloadSum;
    double payloadThroughputAvg;

    long throughputPerSecondMax;
    long payloadThroughputPerSecondMax;

    long activeCallsCountMax;
    long activeCallsLatencyMax;

    public ProfiledCallReport(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "" + getName() + ":" +
                " LatMin: " + latencyMin +
                ", LatMax: " + latencyMax +
                ", LatAvg: " + latencyAvg +
                ", CallsCntSum: " + callsCountSum +
                ", CallsThrptAvg: " + callsThroughputAvg +
                ", RepTimeAvg: " + reportingTimeAvg +
                ", PldMin " + payloadMin +
                ", PldMax " + payloadMax +
                ", PldAvg " + payloadAvg +
                ", PldSum: " + payloadSum +
                ", PldThrptAvg: " + payloadThroughputAvg +
                ", ThrptPerSecMax: " + throughputPerSecondMax +
                ", PldThrptPerSecMax: " + payloadThroughputPerSecondMax +
                ", ActCallsCnt: " + activeCallsCountMax +
                ", ActCallsLatMax: " + activeCallsLatencyMax;
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


    public double getCallsThroughputAvg() {
        return callsThroughputAvg;
    }

    public ProfiledCallReport setCallsThroughputAvg(double callsThroughputAvg) {
        this.callsThroughputAvg = callsThroughputAvg;
        return this;
    }

    public double getPayloadThroughputAvg() {
        return payloadThroughputAvg;
    }

    public ProfiledCallReport setPayloadThroughputAvg(double payloadThroughputAvg) {
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
