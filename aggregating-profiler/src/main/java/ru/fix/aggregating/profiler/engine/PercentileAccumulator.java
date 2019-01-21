package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.PercentileSettings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;

public class PercentileAccumulator {

    final PercentileSettings settings;

    final TreeMap<Long, LongAdder> buckets;


    public PercentileAccumulator(PercentileSettings settings) {
        this.settings = settings;
        buckets = buildBuckets(settings);
    }

    public void accumulate(long latency) {
        Map.Entry<Long, LongAdder> ceilingBucket = buckets.ceilingEntry(latency);
        if (ceilingBucket != null) {
            ceilingBucket.getValue().increment();
        }
    }

    private TreeMap<Long, LongAdder> buildBuckets(PercentileSettings settings) {
        TreeMap<Long, LongAdder> buckets = new TreeMap<>();

        for (int level : settings.getBuckets()) {
            buckets.put((long) level, new LongAdder());
        }
        return buckets;
    }

    public Map<Integer, Long> buildAndReset(long currentMaximum) {

        TreeMap<Long, Long> counts = new TreeMap<>();

        for (Map.Entry<Long, LongAdder> level : buckets.entrySet()) {
            long count = AdderDrainer.drain(level.getValue());
            if (count > 0) {
                counts.put(level.getKey(), count);
            }
        }

        double sum = counts.values().stream().mapToLong(it -> it).sum();


        int[] percentiles = settings.getPercentiles().stream().mapToInt(it -> it).sorted().toArray();
        double[] percentileCounts = Arrays.stream(percentiles)
                .mapToDouble(it -> sum * it / 100)
                .toArray();

        double currentCounts = 0;
        int percentileIndex = 0;

        Map<Integer, Long> report = new HashMap<>();

        for (Map.Entry<Long, Long> level : counts.entrySet()) {
            currentCounts += level.getValue();

            while (percentileIndex < percentiles.length && currentCounts >= percentileCounts[percentileIndex]) {
                report.put(percentiles[percentileIndex], level.getKey());
                percentileIndex++;
            }
        }

        while (percentileIndex < percentiles.length) {
            report.put(percentiles[percentileIndex], currentMaximum);
            percentileIndex++;
        }

        return report;
    }
}
