package ru.fix.aggregating.profiler;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface ThrowableSupplier<R, T extends Throwable> {
    CompletableFuture<R> get(ProfiledCall profiledCall) throws T;
}
