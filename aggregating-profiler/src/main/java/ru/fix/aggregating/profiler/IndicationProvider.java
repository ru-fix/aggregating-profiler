package ru.fix.aggregating.profiler;

/**
 * Provides value of indicator
 */
@FunctionalInterface
public interface IndicationProvider {
    //TODO: change indicator value to double
    Long get() throws Exception;
}
