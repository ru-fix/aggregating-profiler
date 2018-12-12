package ru.fix.aggregating.profiler.engine;

import ru.fix.aggregating.profiler.Identity;

import java.util.function.Consumer;

@FunctionalInterface
public interface CallAggregateMutator {
    void updateAggregate(Identity profiledCallName, Consumer<CallAggregate> updateAction);
}
