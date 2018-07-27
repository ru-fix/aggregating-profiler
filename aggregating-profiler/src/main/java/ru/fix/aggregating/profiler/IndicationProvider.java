package ru.fix.aggregating.profiler;

/**
 * Provides value of indicator
 */
@FunctionalInterface
public interface IndicationProvider {
    Long get() throws Exception;
}
