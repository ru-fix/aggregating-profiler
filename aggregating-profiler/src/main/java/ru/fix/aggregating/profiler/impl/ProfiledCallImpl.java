package ru.fix.aggregating.profiler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.ProfiledCall;
import ru.fix.aggregating.profiler.ThrowableSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

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
    public void call(long startTime, long endTime, long payload) {
        long latencyValue = endTime - startTime;
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
    public void call(long startTime, long endTime) {
        call(startTime, endTime, 1);
    }

    @Override
    public void call(long payload) {
        profiler.applyToSharedCounters(profiledCallName, sharedCounters -> {
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
            throw new IllegalArgumentException("Start method was already called.");
        }
        startTime.set(System.nanoTime());
        profiler.applyToSharedCounters(profiledCallName, sharedCounters -> {
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

        internalStop(payload);
    }

    private void internalStop(long payload) {
        long latencyValue = timeFromCallStartInMs();

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

            sharedCounters.getActiveCalls().remove(this);
            sharedCounters.getActiveCallsCounter().decrement();
        });
    }

    Long startTime() {
        return startTime.get();
    }

    long timeFromCallStartInMs() {
        return (System.nanoTime() - startTime.get()) / 1000000;
    }

    @Override
    public void stopIfRunning(long payload) {
        if (!started.compareAndSet(true, false)) {
            log.debug("stopIfRunning method called on profiler call that currently is not running: {}",
                    profiledCallName);
            return;
        }

        internalStop(payload);
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
    public <R> CompletableFuture<R> profileFuture(Supplier<CompletableFuture<R>> cfSupplier) {
        CompletableFuture<R> future;
        try {
            future = cfSupplier.get();
        } catch (Exception e) {
            close();
            throw e;
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
    public <R, T extends Throwable> CompletableFuture<R> profileFuture(ThrowableSupplier<R, T> futureSupplier) throws T {
        CompletableFuture<R> future;
        try {
            future = futureSupplier.get(this);
        } catch (Throwable e) {
            close();
            throw e;
        }

        return future.whenComplete((res, thr) -> {
            if (thr != null) {
                close();
            } else {
                // ThrowableSupplier uses this profiledCall
                // and it's ok that the profiledCall stopped explicitly
                stopIfRunning();
            }
        });
    }

    @Override
    public void close() {
        if (!started.compareAndSet(true, false)) {
            // do nothing, if not started or stopped already
            return;
        }
        profiler.applyToSharedCounters(profiledCallName, sharedCounters -> {
            sharedCounters.getActiveCalls().remove(this);
            sharedCounters.getActiveCallsCounter().decrement();
        });
    }

    @Override
    public String toString() {
        return profiledCallName;
    }
}
