package ru.fix.commons.profiler.impl;

import ru.fix.commons.profiler.util.CalculateMaxThroughput;

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

    private final CalculateMaxThroughput maxThroughput = new CalculateMaxThroughput();
    private final CalculateMaxThroughput maxPayloadThroughput = new CalculateMaxThroughput();



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

    public CalculateMaxThroughput getMaxThroughput() {
        return maxThroughput;
    }

    public CalculateMaxThroughput getMaxPayloadThroughput() {
        return maxPayloadThroughput;
    }
}
