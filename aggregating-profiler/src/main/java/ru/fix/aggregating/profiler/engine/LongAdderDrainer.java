package ru.fix.aggregating.profiler.engine;

import java.util.concurrent.atomic.LongAdder;

public class LongAdderDrainer {
    public static long drain(LongAdder adder) {
        long sum = adder.sum();
        adder.add(-sum);
        return sum;
    }
}
