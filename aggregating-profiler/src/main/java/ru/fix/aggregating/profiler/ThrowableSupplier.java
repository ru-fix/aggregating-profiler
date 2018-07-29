package ru.fix.aggregating.profiler;

@FunctionalInterface
public interface ThrowableSupplier<R, T extends Throwable> {
    R get() throws T;
}
