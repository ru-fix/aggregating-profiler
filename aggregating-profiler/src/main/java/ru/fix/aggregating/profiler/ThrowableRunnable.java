package ru.fix.aggregating.profiler;

@FunctionalInterface
public interface ThrowableRunnable<T extends Throwable> {
    void run() throws T;
}
