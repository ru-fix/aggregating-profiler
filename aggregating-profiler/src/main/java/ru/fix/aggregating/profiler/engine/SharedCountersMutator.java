package ru.fix.aggregating.profiler.engine;

import java.util.function.Consumer;

@FunctionalInterface
public interface SharedCountersMutator {
    void updateCounters(String profiledCallName, Consumer<SharedCounters> updateAction);
}
