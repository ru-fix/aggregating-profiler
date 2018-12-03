package ru.fix.aggregating.profiler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author Kamil Asfandiyarov
 */
public interface Profiler {

    /**
     * @param name Name of profiling call (e.g name of method about to be profiled)
     *             Method name could be separated by dot '.'
     */
    ProfiledCall profiledCall(String name);

    /**
     * Creates and starts profiled call
     * shortcut of {@code profiledCall(<name>).start()}
     */
    default ProfiledCall start(String name) {
        return profiledCall(name).start();
    }

    /**
     * Creates and starts profiler which measures provided block of code
     *
     * @see ProfiledCall#profile(java.util.function.Supplier)
     */
    default <R> R profile(String name, Supplier<R> block) {
        return profiledCall(name).profile(block);
    }

    /**
     * Creates and starts profiler which measures provided block of code
     *
     * @see ProfiledCall#profile(java.util.function.Supplier)
     */
    default <R, T extends Throwable> R profileThrowable(String name, ThrowableSupplier<R, T> block) throws T {
        return profiledCall(name).profileThrowable(block);
    }

    /**
     * Creates and starts profiler which measures provided block of code
     *
     * @see ProfiledCall#profile(java.lang.Runnable)
     */
    default void profile(String name, Runnable block) {
        profiledCall(name).profile(block);
    }

    /**
     * Measure provided feature execution
     *
     * @param name            name of profiling call
     * @param asyncInvocation CompletableFuture provider
     */
    default <R> CompletableFuture<R> profileFuture(String name, Supplier<CompletableFuture<R>> asyncInvocation) {
        return profiledCall(name).profileFuture(asyncInvocation);
    }

    default <R, T extends Throwable> CompletableFuture<R> profileFutureThrowable(
            String name,
            ThrowableSupplier<CompletableFuture<R>, T> asyncInvocation) throws T {
        return profiledCall(name).profileFutureThrowable(asyncInvocation);
    }

    /**
     * Creates and calls profiled call,
     * shortcut of {@code profiledCall(<name>).call()}
     */
    default void call(String name) {
        profiledCall(name).call();
    }

    /**
     * Add named indicator to profiler.
     *
     * @param name               Name of indicator
     *                           Indicator name could be separated by dot '.'
     * @param indicationProvider Indicator value provider. Must be thread-safe.
     */
    void attachIndicator(String name, IndicationProvider indicationProvider);

    /**
     * Remove indicator
     *
     * @param name Name of indicator
     */
    void detachIndicator(String name);

    /**
     * Create new instance of reporter.
     * Reporter is closable resource
     */
    ProfilerReporter createReporter();

    void setTagger(Tagger tagger);
}
