package ru.fix.aggregating.profiler.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.Identity;
import ru.fix.aggregating.profiler.ProfiledCall;
import ru.fix.aggregating.profiler.ThrowableRunnable;
import ru.fix.aggregating.profiler.ThrowableSupplier;

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

    final AtomicLong startNanoTime = new AtomicLong();

    final CallAggregateMutator aggregateMutator;

    final Identity identity;

    public AggregatingCall(Identity identity, CallAggregateMutator aggregateMutator) {
        this.aggregateMutator = aggregateMutator;
        this.identity = identity;
    }

    @Override
    public void call() {
        aggregateMutator.updateAggregate(
                identity,
                aggregate -> aggregate.call(System.currentTimeMillis(), 0, 1));
    }

    @Override
    public void call(long payload) {
        aggregateMutator.updateAggregate(
                identity,
                aggregate -> aggregate.call(System.currentTimeMillis(), 0, 1));
    }

    @Override
    public void call(long startTime, long payload) {
        Long currentTime = System.currentTimeMillis();

        aggregateMutator.updateAggregate(
                identity,
                aggregate -> aggregate.call(currentTime, currentTime - startTime, payload));
    }

    @Override
    public ProfiledCall start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Start method was already called." +
                    " Profiled call: " + identity);
        }
        startNanoTime.set(System.nanoTime());

        aggregateMutator.updateAggregate(
                identity,
                aggregate -> aggregate.start(this, System.currentTimeMillis()));
        return this;
    }

    @Override
    public void stop(long payload) {
        if (!started.compareAndSet(true, false)) {
            log.warn("Stop method called on profiler call that currently is not running: {}", identity);
            return;
        }

        updateCountersOnStop(payload);
    }

    private void updateCountersOnStop(long payload) {
        long latencyValue = (System.nanoTime() - startNanoTime.get()) / 1000000;

        aggregateMutator.updateAggregate(
                identity,
                aggregate -> aggregate.stop(this, System.currentTimeMillis(), latencyValue, payload));
    }

    public long startNanoTime() {
        return startNanoTime.get();
    }

    public long timeFromCallStart() {
        return (System.nanoTime() - startNanoTime.get()) / 1000000;
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
            start();
            R r = block.get();
            stop();
            return r;
        } finally {
            close();
        }
    }

    @Override
    public <R, T extends Throwable> R profileThrowable(ThrowableSupplier<R, T> block) throws T {
        try {
            start();
            R r = block.get();
            stop();
            return r;
        } finally {
            close();
        }
    }

    @Override
    public <T extends Throwable> void profileThrowable(ThrowableRunnable<T> block) throws T {
        try {
            start();
            block.run();
            stop();
        } finally {
            close();
        }
    }

    @Override
    public void profile(Runnable block) {
        try {
            start();
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
            start();
            future = asyncInvocation.get();
        } catch (Throwable exc) {
            close();
            throw exc;
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
    public <R, T extends Throwable> CompletableFuture<R> profileFutureThrowable(ThrowableSupplier<CompletableFuture<R>, T> asyncInvocation) throws T {
        CompletableFuture<R> future;
        try {
            start();
            future = asyncInvocation.get();
        } catch (Throwable exc) {
            close();
            throw exc;
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
        aggregateMutator.updateAggregate(
                identity,
                aggregate -> aggregate.close(this));
    }

    @Override
    public String toString() {
        return identity.toString();
    }
}
