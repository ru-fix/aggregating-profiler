package ru.fix.aggregating.profiler;

import java.util.Arrays;
import java.util.List;

public class PercentileSettings {
    /**
     * metric name and percentile value, like 98 -> 0.98
     */
    List<Integer> percentiles;

    int bucketCount;

    long historySize = 3;

    public PercentileSettings() {
        percentiles = Arrays.asList(95, 98, 99);
        bucketCount = 10;
    }

    public List<Integer> getPercentiles() {
        return percentiles;
    }

    public PercentileSettings setPercentiles(List<Integer> percentiles) {
        this.percentiles = percentiles;
        return this;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public PercentileSettings setBucketCount(int bucketCount) {
        this.bucketCount = bucketCount;
        return this;
    }
//
//    public Map<Double, String> build(){
//
//    }
}
