package ru.fix.aggregating.profiler.engine;

@FunctionalInterface
public interface ClosingCallback {
    void closed();
}
