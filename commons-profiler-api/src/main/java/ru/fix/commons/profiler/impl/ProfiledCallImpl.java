package ru.fix.commons.profiler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.commons.profiler.ProfiledCall;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kamil Asfandiyarov
 */
class ProfiledCallImpl implements ProfiledCall {
    private static final Logger log = LoggerFactory.getLogger(ProfiledCallImpl.class);

    final AtomicBoolean started = new AtomicBoolean();

    final AtomicLong startTime = new AtomicLong();

    final SimpleProfiler profiler;

    final String profiledCallName;

    ProfiledCallImpl(SimpleProfiler profiler, String profiledCallName) {
        this.profiler = profiler;
        this.profiledCallName = profiledCallName;
    }

    @Override
    public void call() {
        profiler.applyToSharedCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getCallsCount().increment();
            sharedCounters.getMaxThroughput().call();
        });
    }

    @Override
    public ProfiledCall start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalArgumentException("Start method was already called.");
        }
        startTime.set(System.nanoTime());
        return this;
    }

    @Override
    public void stop() {
        stop(1);
    }

    @Override
    public void stop(long payload) {
        if (!started.compareAndSet(true, false)) {
            log.debug("Stop method called on profiler call that currently is not running: {}", profiledCallName);
            return;
        }

        long stopTime = System.nanoTime();
        long latencyValue = (stopTime - startTime.get()) / 1000000;

        profiler.applyToSharedCounters(profiledCallName, sharedCounters -> {
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
    public boolean isStopped() {
        return !started.get();
    }
}
