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

    /**
     * @param currentMaximum maximum value of the metric that Reporter saw during reporting period
     *                       minor optimization so PercentileAccumulator do not need to track this value by itself
     */
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

        // currentCounts maintain total count of measurements
        // each bucket will increase currentCounts level
        // if there are 200 measurement in total
        // then 90 percentile is value of 180th measure
        //      95 percentile is value of 190th measure
        // suppose that our buckets holds:
        // bucket[0] = 100
        // bucket[1] = 40
        // bucket[2] = 120
        //
        // the currentValue will be:
        // level 0 is 100
        // level 1 is 140
        // level 2 is 260
        //
        // for level 0 and 1 currentValue is not enough to fill report
        // for level 2 currentCounts becomes 260. It is bigger than both of percentiles: 180(for 90p) and 190(for 95p).
        // We will set same 260th measure value for both percentiles in report.
        // This will lead to a situation when 90 percentile is equal to 95 percentile.
        // 90 percentile is bigger that real value. But due to inaccuracy we fill it at least with
        // not accurate value that we have.
        //
        // Actual percentile value is less or equal to the one that we are reporting.
        //
        // TODO: add to readme
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
