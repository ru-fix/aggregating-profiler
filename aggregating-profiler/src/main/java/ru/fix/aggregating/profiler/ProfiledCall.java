package ru.fix.aggregating.profiler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author Kamil Asfandiyarov
 */
public interface ProfiledCall extends AutoCloseable {

    /**
     * If you want to evaluate calls count or whether the method was called
     */
    void call();

    /**
     * Call with payload ( = 1) and start-stop time
     */
    void call(long startTime, long endTime);

    /**
     * Call with payload and start-stop time
     */
    void call(long startTime, long endTime, long payload);

    /**
     * Call with payload
     */
    void call(long payload);

    /**
     * if you want to know some metrics then you should start and stop profiled call
     *
     * @throws IllegalStateException if method start called twice
     */
    ProfiledCall start();

    /**
     * Same as stop(1)
     *
     * @see #stop(long)
     */
    default void stop() {
        stop(1);
    }

    /**
     * Call if profiled code executed normally. Applies all measurement to overall metrics.
     */
    void stop(long payload);

    /**
     * Stopping profiler unconditionally (without warnings or exceptions)
     */
    default void stopIfRunning() {
        stopIfRunning(1);
    }

    /**
     * Stopping profiler unconditionally with payload (without warnings or exceptions)
     */
    void stopIfRunning(long payload);

    /**
     * Profile provided block of code which returns some result
     */
    <R> R profile(Supplier<R> block);

    /**
     * Profile provided block of code without result
     */
    void profile(Runnable block);

    /**
     * Profile provided future
     */
    <R> CompletableFuture<R> profileFuture(Supplier<CompletableFuture<R>> asyncInvocation);

    /**
     * Profile provided future
     */
    <R, T extends Throwable> CompletableFuture<R> profileFutureThrowable(
            ThrowableSupplier<CompletableFuture<R>, T> asyncInvocation) throws T;

    /**
     * Call if profiled code didn't execute normally and it's measurements must be discarded.
     * Useful if profiled code failed fast and must not be displayed in latency metrics because it will throw it off.
     */
    @Override
    void close();

}
