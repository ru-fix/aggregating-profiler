package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.PercentileSettings;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;

public class PercentileAccumulator {

    final HistoricalMinMax historicalMinMax;
    final PercentileSettings settings;

    final TreeMap<Long, LongAdder> buckets = new TreeMap<>();

    public PercentileAccumulator(PercentileSettings settings) {
        this.settings = settings;
        historicalMinMax = new HistoricalMinMax(settings);


    }

    public void accumulate(long latency) {
        buckets.ceilingEntry(latency).getValue().add(latency);
    }

    public Map<String, Long> buildAndReset(long min, long max) {

        historicalMinMax.updateMinMax(min, max);


        settings.getBucketCount()




        long observationCount = 0;
        long bucketCounts[] = new long[]{};
        long bucketBorders[] = new long[]{};

        long sum = 0;
        for (int i = 0; i < bucketCounts.length; i++) {
            sum += bucketCounts[i];
        }


        for (int i = 0; i < bucketCounts.length; i++) {
        }

        for(Map.Entry<Long, LongAdder> entry : buckets.entrySet()){
            long bucketValue = AdderDrainer.drain(entry.getValue());
        }



        return null;
    }
}
