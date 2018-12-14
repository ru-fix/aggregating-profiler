package ru.fix.aggregating.profiler;

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
        public void call(double payload) {
        }

        @Override
        public void call(long startTime, double payload) {

        }

        @Override
        public ProfiledCall start() {
            return this;
        }

        @Override
        public void stop(double payload) {
        }

        @Override
        public void close() {
        }

        @Override
        public void stop() {

        }

        @Override
        public void stopIfRunning(double payload) {
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
        public <R, T extends Throwable> R profileThrowable(ThrowableSupplier<R, T> block) throws T {
            return block.get();
        }

        @Override
        public <T extends Throwable> void profileThrowable(ThrowableRunnable<T> block) throws T {
            block.run();
        }

        @Override
        public <R> CompletableFuture<R> profileFuture(Supplier<CompletableFuture<R>> asyncInvocation) {
            return asyncInvocation.get();
        }

        @Override
        public <R, T extends Throwable> CompletableFuture<R> profileFutureThrowable(
                ThrowableSupplier<CompletableFuture<R>, T> asyncInvocation) throws T {
            return asyncInvocation.get();
        }
    }

    @Override
    public ProfiledCall profiledCall(String name) {
        return new NoopProfiledCall();
    }

    @Override
    public ProfiledCall profiledCall(Identity identity) {
        return new NoopProfiledCall();
    }

    @Override
    public void attachIndicator(String name, IndicationProvider indicationProvider) {
    }

    @Override
    public void detachIndicator(String name) {
    }

    public void setLabelSticker(LabelSticker labelSticker) {
        //no need any changes
    }

    @Override
    public ProfilerReporter createReporter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachIndicator(Identity identity, IndicationProvider indicationProvider) {

    }

    @Override
    public void detachIndicator(Identity identity) {

    }
}
