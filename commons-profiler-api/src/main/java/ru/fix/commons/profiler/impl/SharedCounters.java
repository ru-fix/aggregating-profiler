package ru.fix.commons.profiler.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import ru.fix.commons.profiler.MetricsGroupTag;

/**
 * @author Kamil Asfandiyarov
 */
class SharedCounters {
    private final LongAdder callsCount = new LongAdder();
    private final LongAdder startedCallsCount = new LongAdder();
    private final LongAdder sumStartStopLatency = new LongAdder();

    private final AtomicLong latencyMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong latencyMax = new AtomicLong();

    private final AtomicLong payloadMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong payloadMax = new AtomicLong();
    private final LongAdder payloadSum = new LongAdder();

    private final MaxThroughputCalculator maxThroughput = new MaxThroughputCalculator();
    private final MaxThroughputCalculator maxPayloadThroughput = new MaxThroughputCalculator();

    private final LongAdder activeCallsCounter = new LongAdder();
    private final ActiveCallsSet activeCalls = new ActiveCallsSet();

    private final AtomicBoolean recordActiveCalls = new AtomicBoolean();

    private final MetricsGroupTag groupTag = new MetricsGroupTag();
    
    public SharedCounters(boolean recordActiveCalls) {
        this.recordActiveCalls.set(recordActiveCalls);
    }

    public MetricsGroupTag getGroupTag() {
        return this.groupTag;
    }

    void setRecordActiveCalls(boolean recordActiveCalls) {
        this.recordActiveCalls.set(recordActiveCalls);
    }

    public LongAdder getCallsCount() {
        return callsCount;
    }

    public LongAdder getStartedCallsCount() {
        return startedCallsCount;
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

    public LongAdder getActiveCallsCounter() {
        return activeCallsCounter;
    }

    public ActiveCallsSet getActiveCalls() {
        return activeCalls;
    }

    public class ActiveCallsSet implements Iterable<ProfiledCallImpl> {
        private ConcurrentHashMap<ProfiledCallImpl, Boolean> activeCalls = new ConcurrentHashMap<>();

        public void add(ProfiledCallImpl call) {
            if (recordActiveCalls.get()) {
                activeCalls.put(call, true);
            }
        }

        public void remove(ProfiledCallImpl call) {
            activeCalls.remove(call);
        }

        public boolean isEmpty() {
            return activeCalls.isEmpty();
        }

        public int size() {
            return activeCalls.size();
        }

        public boolean containsAll(Collection<?> collection) {
            return activeCalls.keySet().containsAll(collection);
        }

        @Override
        public Iterator<ProfiledCallImpl> iterator() {
            return activeCalls.keySet().iterator();
        }

        public Stream<ProfiledCallImpl> stream() {
            return activeCalls.keySet().stream();
        }

        public void reset() {
            activeCalls.clear();
        }
    }
}
