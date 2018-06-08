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
     */
    default ProfiledCall startProfiledCall(String name) {
        return profiledCall(name).start();
    }

    default <T> CompletableFuture<T> profiledCall(String name, Supplier<CompletableFuture<T>> cfSupplier) {
        ProfiledCall call = startProfiledCall(name);
        CompletableFuture<T> future;
        try {
            future = cfSupplier.get();
        } catch (Exception e) {
            call.cancel();
            throw e;
        }
        return future.whenComplete((res, thr) -> call.stop());
    }

    default void makeCall(String name) {
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
