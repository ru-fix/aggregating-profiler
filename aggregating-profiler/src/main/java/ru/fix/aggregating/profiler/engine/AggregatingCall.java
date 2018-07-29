package ru.fix.aggregating.profiler.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.ProfiledCall;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * @author Kamil Asfandiyarov
 */
public class AggregatingCall implements ProfiledCall {
    private static final Logger log = LoggerFactory.getLogger(AggregatingCall.class);

    final AtomicBoolean started = new AtomicBoolean();

    final AtomicLong startTime = new AtomicLong();

    final SharedCountersMutator mutator;

    final String profiledCallName;

    public AggregatingCall(String profiledCallName, SharedCountersMutator mutator) {
        this.mutator = mutator;
        this.profiledCallName = profiledCallName;
    }

    @Override
    public void call() {
        mutator.updateCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getCallsCount().increment();
            sharedCounters.getMaxThroughput().call();
        });
    }

    @Override
    public void call(long startTime, long endTime, long payload) {
        long latencyValue = endTime - startTime;
        mutator.updateCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getCallsCount().increment();

            sharedCounters.getSumStartStopLatency().add(latencyValue);
            sharedCounters.getLatencyMin().accumulateAndGet(latencyValue, Math::min);
            sharedCounters.getLatencyMax().accumulateAndGet(latencyValue, Math::max);

            sharedCounters.getPayloadMin().accumulateAndGet(payload, Math::min);
            sharedCounters.getPayloadMax().accumulateAndGet(payload, Math::max);
            sharedCounters.getPayloadSum().add(payload);
            sharedCounters.getMaxThroughput().call();
            sharedCounters.getMaxPayloadThroughput().call(payload);
        });
    }

    @Override
    public void call(long startTime, long endTime) {
        call(startTime, endTime, 1);
    }

    @Override
    public void call(long payload) {
        mutator.updateCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getCallsCount().increment();
            sharedCounters.getMaxThroughput().call();

            sharedCounters.getPayloadMin().accumulateAndGet(payload, Math::min);
            sharedCounters.getPayloadMax().accumulateAndGet(payload, Math::max);
            sharedCounters.getPayloadSum().add(payload);
            sharedCounters.getMaxPayloadThroughput().call(payload);
        });
    }

    @Override
    public ProfiledCall start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalArgumentException("Start method was already called." +
                    " Profiler: " + profiledCallName);
        }
        startTime.set(System.nanoTime());
        mutator.updateCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getStartedCallsCount().increment();

            sharedCounters.getActiveCalls().add(this);
            sharedCounters.getActiveCallsCounter().increment();
        });
        return this;
    }

    @Override
    public void stop(long payload) {
        if (!started.compareAndSet(true, false)) {
            log.warn("Stop method called on profiler call that currently is not running: {}", profiledCallName);
            return;
        }

        updateCountersOnStop(payload);
    }

    private void updateCountersOnStop(long payload) {
        long latencyValue = (System.nanoTime() - startTime.get()) / 1000000;

        mutator.updateCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getCallsCount().increment();

            sharedCounters.getSumStartStopLatency().add(latencyValue);
            sharedCounters.getLatencyMin().accumulateAndGet(latencyValue, Math::min);
            sharedCounters.getLatencyMax().accumulateAndGet(latencyValue, Math::max);


            sharedCounters.getPayloadMin().accumulateAndGet(payload, Math::min);
            sharedCounters.getPayloadMax().accumulateAndGet(payload, Math::max);
            sharedCounters.getPayloadSum().add(payload);
            sharedCounters.getMaxThroughput().call();
            sharedCounters.getMaxPayloadThroughput().call(payload);

            sharedCounters.getActiveCalls().remove(this);
            sharedCounters.getActiveCallsCounter().decrement();
        });
    }

    long timeFromCallStart() {
        return (System.nanoTime() - startTime.get()) / 1000000;
    }

    @Override
    public void stopIfRunning(long payload) {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        updateCountersOnStop(payload);
    }

    @Override
    public <R> R profile(Supplier<R> block) {
        try {
            R r = block.get();
            stop();
            return r;
        } finally {
            close();
        }
    }

    @Override
    public void profile(Runnable block) {
        try {
            block.run();
            stop();
        } finally {
            close();
        }
    }

    @Override
    public <R> CompletableFuture<R> profileFuture(Supplier<CompletableFuture<R>> asyncInvocation) {
        CompletableFuture<R> future;
        try {
            future = asyncInvocation.get();
        } finally {
            close();
        }

        return future.whenComplete((res, thr) -> {
            if (thr != null) {
                close();
            } else {
                stop();
            }
        });
    }


    @Override
    public void close() {
        if (!started.compareAndSet(true, false)) {
            // do nothing, if not started or stopped already
            return;
        }
        mutator.updateCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getActiveCalls().remove(this);
            sharedCounters.getActiveCallsCounter().decrement();
        });
    }

    @Override
    public String toString() {
        return profiledCallName;
    }
}
