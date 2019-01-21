package ru.fix.aggregating.profiler;

import java.util.Arrays;
import java.util.List;

public class PercentileSettings {
    /**
     * (1 - 100)
     * 50 means median 50%
     * 95 means 95%
     */
    List<Integer> percentiles;
    List<Integer> buckets;


    public PercentileSettings() {
        percentiles = Arrays.asList(90, 95, 99);
        buckets = Arrays.asList(
                3,
                5,
                10,
                25,
                50,
                100,
                250,
                375,
                500,
                750,
                1_000,
                1_500,
                3_000,
                6_000,
                12_000,
                30_000,
                60_000
        );
    }

    public List<Integer> getPercentiles() {
        return percentiles;
    }

    public PercentileSettings setPercentiles(List<Integer> percentiles) {
        this.percentiles = percentiles;
        return this;
    }

    public List<Integer> getBuckets() {
        return buckets;
    }

    public PercentileSettings setBuckets(List<Integer> buckets) {
        this.buckets = buckets;
        return this;
    }
}
