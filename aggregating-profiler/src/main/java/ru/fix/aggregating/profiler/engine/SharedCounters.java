package ru.fix.aggregating.profiler.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

/**
 * @author Kamil Asfandiyarov
 */
public class SharedCounters {
    private final LongAdder callsCount = new LongAdder();
    private final LongAdder startedCallsCount = new LongAdder();
    private final LongAdder sumStartStopLatency = new LongAdder();

    private final AtomicLong latencyMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong latencyMax = new AtomicLong();

    private final AtomicLong payloadMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong payloadMax = new AtomicLong();
    private final LongAdder payloadSum = new LongAdder();

    private final MaxThroughputPerSecondAccumulator maxThroughput = new MaxThroughputPerSecondAccumulator();
    private final MaxThroughputPerSecondAccumulator maxPayloadThroughput = new MaxThroughputPerSecondAccumulator();

    private final LongAdder activeCallsCounter = new LongAdder();
    private final ActiveCallsSet activeCalls = new ActiveCallsSet();

    private final AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports;

    public SharedCounters(AtomicInteger numberOfActiveCallsToTrackAndKeepBetweenReports) {
        this.numberOfActiveCallsToTrackAndKeepBetweenReports = numberOfActiveCallsToTrackAndKeepBetweenReports;
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

    public MaxThroughputPerSecondAccumulator getMaxThroughput() {
        return maxThroughput;
    }

    public MaxThroughputPerSecondAccumulator getMaxPayloadThroughput() {
        return maxPayloadThroughput;
    }

    public LongAdder getActiveCallsCounter() {
        return activeCallsCounter;
    }

    public ActiveCallsSet getActiveCalls() {
        return activeCalls;
    }

    public class ActiveCallsSet implements Iterable<AggregatingCall> {
        private ConcurrentHashMap<AggregatingCall, Boolean> activeCalls = new ConcurrentHashMap<>();

        public void add(AggregatingCall call) {
            if (numberOfActiveCallsToTrackAndKeepBetweenReports.get() > 0) {
                activeCalls.put(call, true);
            }
        }

        public void remove(AggregatingCall call) {
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
        public Iterator<AggregatingCall> iterator() {
            return activeCalls.keySet().iterator();
        }

        public Stream<AggregatingCall> stream() {
            return activeCalls.keySet().stream();
        }

        public void reset() {
            activeCalls.clear();
        }
    }
}
