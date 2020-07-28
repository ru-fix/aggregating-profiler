package ru.fix.aggregating.profiler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author Kamil Asfandiyarov
 */
public interface ProfiledCall extends AutoCloseable {

    /**
     * Evaluate calls count or whether the method was called.
     */
    void call();

    /**
     * Call with payload
     */
    void call(double payload);

    /**
     * Call when measured time span just finished and it is known at what time it was started
     * @param startTime
     */
    void call(long startTime);

    /**
     * @param startTime when call started in ms.
     *                  Latency will be calculated as time span between {@link System#currentTimeMillis()} and startTime
     * @param payload
     */
    void call(long startTime, double payload);

    /**
     * if you want to know some metrics then you should start and stop profiled call
     *
     * @throws IllegalStateException if method start called twice
     */
    ProfiledCall start();
//TODO: implement stop and stop(value) in different way, so in case of stop() there will be no Paylaod* metrics reported

    /**
     * Stop profiled call without any payload
     */
    void stop();

    /**
     * Call if profiled code executed normally. Applies all measurement to overall metrics.
     */
    void stop(double payload);

    /**
     * Stopping profiler unconditionally (without warnings or exceptions)
     */
    default void stopIfRunning() {
        stopIfRunning(1);
    }

    /**
     * Stopping profiler unconditionally with payload (without warnings or exceptions)
     */
    void stopIfRunning(double payload);

    /**
     * Profile provided block of code which returns some result
     */
    <R> R profile(Supplier<R> block);

    /**
     * Profile provided block of code which returns some result or can throw an exception
     */
    <R, T extends Throwable> R profileThrowable(ThrowableSupplier<R, T> block) throws T;

    /**
     * Profile provided block of code which returns some result or can throw an exception
     */
    <T extends Throwable> void profileThrowable(ThrowableRunnable<T> block) throws T;

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
