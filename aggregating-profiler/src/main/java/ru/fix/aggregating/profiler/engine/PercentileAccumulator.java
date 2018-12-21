package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.PercentileSettings;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public class PercentileAccumulator {

    final HistoricalMinMax historicalMinMax;
    final PercentileSettings settings;

    final AtomicReference<TreeMap<Long, LongAdder>> currentBuckets = new AtomicReference<>(null);

    public PercentileAccumulator(PercentileSettings settings) {
        this.settings = settings;
        historicalMinMax = new HistoricalMinMax(settings);
    }

    public void accumulate(long latency) {
        TreeMap<Long, LongAdder> buckets = currentBuckets.get();
        if (buckets != null) {
            buckets.ceilingEntry(latency).getValue().add(latency);
        }
    }

    private TreeMap<Long, LongAdder> buildBuckets(long max) {
        TreeMap<Long, LongAdder> buckets = new TreeMap<>();
        buckets.put(Long.MAX_VALUE, new LongAdder());

        long bucketSize = max / settings.getBucketCount();
        long level = bucketSize;

        for (int bucket = 0; bucket < settings.getBucketCount(); bucket++) {
            buckets.put(level, new LongAdder());
            level += bucketSize;
        }

        return buckets;
    }

    public Map<String, Long> buildAndReset(long min, long max) {

        historicalMinMax.updateMinMax(min, max);

        TreeMap<Long, LongAdder> buckets = currentBuckets.getAndSet(buildBuckets(max));

        long aboveMax = AdderDrainer.drain(buckets.remove(Long.MAX_VALUE));
        long sum = buckets.values().stream().mapToLong(AdderDrainer::drain).sum();

        for (int p : settings.getPercentiles()) {
            Double.valueOf("0."+p) * sum
        }


        for(Map.Entry<Long, LongAdder> level : buckets.entrySet()){
            values[][level.getKey()] = AdderDrainer.drain(level.getValue());

        }

        settings.getPercentiles()

    }
}
