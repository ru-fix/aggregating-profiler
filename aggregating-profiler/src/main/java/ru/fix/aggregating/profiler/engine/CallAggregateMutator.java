package ru.fix.aggregating.profiler.engine;

import java.util.function.Consumer;

@FunctionalInterface
public interface CallAggregateMutator {
    void updateAggregate(Identity profiledCallName, Consumer<CallAggregate> updateAction);
}
