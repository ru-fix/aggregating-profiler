package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.PercentileSettings;

public class HistoricalMinMax {

    public HistoricalMinMax(PercentileSettings settings
    ) {
    }

    public void updateMinMax(long min, long max) {
        throw new UnsupportedOperationException();
    }

    public long getMin(){
        throw new UnsupportedOperationException();
    }

    public long getMax(){
        throw new UnsupportedOperationException();
    }
}
