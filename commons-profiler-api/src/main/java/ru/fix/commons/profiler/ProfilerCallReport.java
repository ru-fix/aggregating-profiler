package ru.fix.commons.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public class ProfilerCallReport {

    final String name;

    long minLatency;
    long maxLatency;
    long avgLatency;

    long callsThroughput;
    long callsCount;

    long reportingTime;

    long payloadMin;
    long payloadMax;
    long payloadAvg;
    long payloadTotal;
    long payloadThroughput;

    long maxThroughputPerSecond;
    long maxPayloadThroughputPerSecond;

    public ProfilerCallReport(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s: LatMin: %d, LatMax: %d, LatAvg: %d," +
                        " CallsCnt: %d, CallsThrpt: %d, RepTime: %d," +
                        " PldMin %d, PldMax %d, PldAvg %d," +
                        " PldTot: %d, PldThrpt: %d," +
                        " MaxThrpt: %d, MaxPldThrpt: %d",
                getName(),
                minLatency,
                maxLatency,
                avgLatency,

                callsCount,
                callsThroughput,
                reportingTime,

                payloadMin,
                payloadMax,
                payloadAvg,

                payloadTotal,
                payloadThroughput,

                maxThroughputPerSecond,
                maxPayloadThroughputPerSecond);
    }

    public String getName() {
        return name;
    }

    public long getMinLatency() {
        return minLatency;
    }

    public long getMaxLatency() {
        return maxLatency;
    }

    public long getAvgLatency() {
        return avgLatency;
    }


    public long getReportingTime() {
        return reportingTime;
    }

    public ProfilerCallReport setMinLatency(long minLatency) {
        this.minLatency = minLatency;
        return this;
    }

    public ProfilerCallReport setMaxLatency(long maxLatency) {
        this.maxLatency = maxLatency;
        return this;
    }

    public ProfilerCallReport setAvgLatency(long avgLatency) {
        this.avgLatency = avgLatency;
        return this;
    }


    public ProfilerCallReport setReportingTime(long reportingTime) {
        this.reportingTime = reportingTime;
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

    public long getPayloadTotal() {
        return payloadTotal;
    }

    public ProfilerCallReport setPayloadTotal(long payloadTotal) {
        this.payloadTotal = payloadTotal;
        return this;
    }

    public long getCallsCount() {
        return callsCount;
    }

    public ProfilerCallReport setCallsCount(long callsCount) {
        this.callsCount = callsCount;
        return this;
    }

    public long getCallsThroughput() {
        return callsThroughput;
    }

    public ProfilerCallReport setCallsThroughput(long callsThroughput) {
        this.callsThroughput = callsThroughput;
        return this;
    }

    public long getPayloadThroughput() {
        return payloadThroughput;
    }

    public ProfilerCallReport setPayloadThroughput(long payloadThroughput) {
        this.payloadThroughput = payloadThroughput;
        return this;
    }

    public long getMaxThroughputPerSecond() {
        return maxThroughputPerSecond;
    }

    public ProfilerCallReport setMaxThroughputPerSecond(long maxThroughputPerSecond) {
        this.maxThroughputPerSecond = maxThroughputPerSecond;
        return this;
    }

    public long getMaxPayloadThroughputPerSecond() {
        return maxPayloadThroughputPerSecond;
    }

    public ProfilerCallReport setMaxPayloadThroughputPerSecond(long maxPayloadThroughputPerSecond) {
        this.maxPayloadThroughputPerSecond = maxPayloadThroughputPerSecond;
        return this;
    }
}
