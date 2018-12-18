package ru.fix.aggregating.profiler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Kamil Asfandiyarov
 */
public class ProfiledCallReport {

    final Identity identity;

    long latencyMin;
    long latencyMax;
    long latencyAvg;

    Map<String, Long> latencyPercentile;

    double stopThroughputAvg;
    long stopSum;

    long startSum;
    double startThroughputAvg;
    long startThroughputPerSecondMax;


    long reportingTimeAvg;

    double payloadMin;
    double payloadMax;
    double payloadAvg;
    double payloadSum;
    double payloadThroughputAvg;

    long stopThroughputPerSecondMax;

    long activeCallsCountMax;
    long activeCallsLatencyMax;


    public ProfiledCallReport(Identity identity) {
        this.identity = identity;
    }

    @Override
    public String toString() {
        return asMap().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", ", "" + getIdentity() + ": ", ""));
    }

    public Identity getIdentity() {
        return identity;
    }

    public Map<String, Number> asMap() {
        HashMap<String, Number> map = new HashMap<>();
        map.put("reportingTimeAvg", reportingTimeAvg);

        map.put("startSum", startSum);
        map.put("startThroughputAvg", startThroughputAvg);
        map.put("startThroughputPerSecondMax", startThroughputPerSecondMax);

        map.put("latencyMin", latencyMin);
        map.put("latencyMax", latencyMax);
        map.put("latencyAvg", latencyAvg);

        map.put("activeCallsCountMax", activeCallsCountMax);
        map.put("activeCallsLatencyMax", activeCallsLatencyMax);

        map.put("payloadMin", payloadMin);
        map.put("payloadMax", payloadMax);
        map.put("payloadAvg", payloadAvg);
        map.put("payloadSum", payloadSum);
        map.put("payloadThroughputAvg", payloadThroughputAvg);

        map.put("stopSum", stopSum);
        map.put("stopThroughputAvg", stopThroughputAvg);
        map.put("stopThroughputPerSecondMax", stopThroughputPerSecondMax);

        return map;
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

    public long getStartSum() {
        return startSum;
    }

    public ProfiledCallReport setStartSum(long startSum) {
        this.startSum = startSum;
        return this;
    }

    public double getStartThroughputAvg() {
        return startThroughputAvg;
    }

    public ProfiledCallReport setStartThroughputAvg(double startThroughputAvg) {
        this.startThroughputAvg = startThroughputAvg;
        return this;
    }

    public long getStartThroughputPerSecondMax() {
        return startThroughputPerSecondMax;
    }

    public ProfiledCallReport setStartThroughputPerSecondMax(long startThroughputPerSecondMax) {
        this.startThroughputPerSecondMax = startThroughputPerSecondMax;
        return this;
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

    public double getPayloadMin() {
        return payloadMin;
    }

    public ProfiledCallReport setPayloadMin(double payloadMin) {
        this.payloadMin = payloadMin;
        return this;
    }

    public double getPayloadMax() {
        return payloadMax;
    }

    public ProfiledCallReport setPayloadMax(double payloadMax) {
        this.payloadMax = payloadMax;
        return this;
    }

    public double getPayloadAvg() {
        return payloadAvg;
    }

    public ProfiledCallReport setPayloadAvg(double payloadAvg) {
        this.payloadAvg = payloadAvg;
        return this;
    }

    public double getPayloadSum() {
        return payloadSum;
    }

    public ProfiledCallReport setPayloadSum(double payloadSum) {
        this.payloadSum = payloadSum;
        return this;
    }

    public long getStopSum() {
        return stopSum;
    }

    public ProfiledCallReport setStopSum(long stopSum) {
        this.stopSum = stopSum;
        return this;
    }


    public double getStopThroughputAvg() {
        return stopThroughputAvg;
    }

    public ProfiledCallReport setStopThroughputAvg(double stopThroughputAvg) {
        this.stopThroughputAvg = stopThroughputAvg;
        return this;
    }

    public double getPayloadThroughputAvg() {
        return payloadThroughputAvg;
    }

    public ProfiledCallReport setPayloadThroughputAvg(double payloadThroughputAvg) {
        this.payloadThroughputAvg = payloadThroughputAvg;
        return this;
    }

    public long getStopThroughputPerSecondMax() {
        return stopThroughputPerSecondMax;
    }

    public ProfiledCallReport setStopThroughputPerSecondMax(long stopThroughputPerSecondMax) {
        this.stopThroughputPerSecondMax = stopThroughputPerSecondMax;
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

    public Map<String, Long> getLatencyPercentile() {
        return latencyPercentile;
    }

    public ProfiledCallReport setLatencyPercentile(Map<String, Long> latencyPercentile) {
        this.latencyPercentile = latencyPercentile;
        return this;
    }
}
