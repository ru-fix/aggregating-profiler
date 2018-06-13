package ru.fix.commons.profiler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Test purpose stub
 */
public class NoopProfiler implements Profiler {

    public static class NoopProfiledCall implements ProfiledCall {
        @Override
        public void call() {
        }

        @Override
        public void call(long payload) {
        }

        @Override
        public ProfiledCall start() {
            return this;
        }

        @Override
        public void stop(long payload) {
        }

        @Override
        public void close() {
        }

        @Override
        public void stopIfRunning(long payload) {
        }

        @Override
        public <R> R profile(Supplier<R> block) {
            return block.get();
        }

        @Override
        public void profile(Runnable block) {
            block.run();
        }

        @Override
        public <R> CompletableFuture<R> profileFuture(Supplier<CompletableFuture<R>> cfSupplier) {
            return cfSupplier.get();
        }

        @Override
        public <R, T extends Throwable> CompletableFuture<R> profileFuture(ThrowableSupplier<R, T> futureSupplier) throws T {
            return futureSupplier.get(this);
        }
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return new NoopProfiledCall();
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
    }

    @Override
    public void detachIndicator(String name) {
    }

    @Override
    public ProfilerReporter createReporter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProfilerReporter createReporter(boolean enableActiveCallsMaxLatency, int activeCallsToKeepBetweenReports) {
        throw new UnsupportedOperationException();
    }
}
