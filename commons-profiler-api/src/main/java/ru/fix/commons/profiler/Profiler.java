package ru.fix.commons.profiler;

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
     * Measure provided feature execution
     *
     * @param name       name of profiling call
     * @param cfSupplier CompletableFuture provider
     */
    default <R> CompletableFuture<R> profileFuture(String name, Supplier<CompletableFuture<R>> cfSupplier) {
        ProfiledCall call = start(name);
        CompletableFuture<R> future;
        try {
            future = cfSupplier.get();
        } catch (Exception e) {
            call.close();
            throw e;
        }

        return future.whenComplete((res, thr) -> {
            if (thr != null) {
                // drop the call
                call.close();
            } else {
                call.stop();
            }
        });
    }

    default <R> R profile(String name, Supplier<R> block) {
        ProfiledCall call = start(name);
        try {
            R r = block.get();
            call.stop();
            return r;
        } finally {
            // drop
            call.close();
        }
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

    /**
     * Create new instance of reporter.
     * Reporter is closable resource.
     *
     * @param enableActiveCallsMaxLatency     see {@link ProfilerReporter#setEnableActiveCallsMaxLatency(boolean)}
     * @param activeCallsToKeepBetweenReports see {@link ProfilerReporter#setNumberOfActiveCallsToKeepBetweenReports(int)}
     */
    ProfilerReporter createReporter(boolean enableActiveCallsMaxLatency, int activeCallsToKeepBetweenReports);
}
