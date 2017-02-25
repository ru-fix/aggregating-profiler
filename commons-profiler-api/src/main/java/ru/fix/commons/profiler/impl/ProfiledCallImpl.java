package ru.fix.commons.profiler.impl;

import ru.fix.commons.profiler.ProfiledCall;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kamil Asfandiyarov
 */
class ProfiledCallImpl implements ProfiledCall {

    final AtomicBoolean started = new AtomicBoolean();

    final AtomicLong startTime = new AtomicLong();

    final SimpleProfiler profiler;

    final String profiledCallName;

    ProfiledCallImpl(SimpleProfiler profiler, String profiledCallName) {
        this.profiler = profiler;
        this.profiledCallName = profiledCallName;
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


        long stopTime = System.nanoTime();
        long latencyValue = (stopTime - startTime.get()) / 1000000;
        if (!started.compareAndSet(true, false)) {
            throw new IllegalArgumentException("Stop method was already called.");
        }

        for (SharedCounters sharedCounters : profiler.getSharedCounters(profiledCallName)) {

            sharedCounters.getCallsCount().increment();

            sharedCounters.getSumStartStopLatency().add(latencyValue);
            sharedCounters.getLatencyMin().accumulateAndGet(latencyValue, Math::min);
            sharedCounters.getLatencyMax().accumulateAndGet(latencyValue, Math::max);


            sharedCounters.getPayloadMin().accumulateAndGet(payload, Math::min);
            sharedCounters.getPayloadMax().accumulateAndGet(payload, Math::max);
            sharedCounters.getPayloadSum().add(payload);
        }
    }

    @Override
    public boolean isStopped() {
        return !started.get();
    }
}
