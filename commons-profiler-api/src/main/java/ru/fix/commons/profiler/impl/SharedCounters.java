package ru.fix.commons.profiler.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Kamil Asfandiyarov
 */
class SharedCounters {
    private final LongAdder callsCount = new LongAdder();
    private final LongAdder sumStartStopLatency = new LongAdder();
    
    private final AtomicLong latencyMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong latencyMax = new AtomicLong();
    
    private final AtomicLong payloadMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong payloadMax = new AtomicLong();
    private final LongAdder payloadSum = new LongAdder();

    private final MaxThroughputCalculator maxThroughput = new MaxThroughputCalculator();
    private final MaxThroughputCalculator maxPayloadThroughput = new MaxThroughputCalculator();



    public LongAdder getCallsCount() {
        return callsCount;
    }

    public LongAdder getSumStartStopLatency() {
        return sumStartStopLatency;
    }

    public AtomicLong getLatencyMin() {
        return latencyMin;
    }

    public AtomicLong getLatencyMax() {
        return latencyMax;
    }

    public AtomicLong getPayloadMin() {
        return payloadMin;
    }

    public AtomicLong getPayloadMax() {
        return payloadMax;
    }

    public LongAdder getPayloadSum() {
        return payloadSum;
    }

    public MaxThroughputCalculator getMaxThroughput() {
        return maxThroughput;
    }

    public MaxThroughputCalculator getMaxPayloadThroughput() {
        return maxPayloadThroughput;
    }
}
