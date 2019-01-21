package ru.fix.aggregating.profiler.engine;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

public class AdderDrainer {
    public static long drain(LongAdder adder) {
        long sum = adder.sum();
        adder.add(-sum);
        return sum;
    }

    public static double drain(DoubleAdder adder) {
        double sum = adder.sum();
        adder.add(-sum);
        return sum;
    }
}
