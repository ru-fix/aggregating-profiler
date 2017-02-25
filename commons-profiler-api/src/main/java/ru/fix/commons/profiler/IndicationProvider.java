package ru.fix.commons.profiler;

/**
 * Provides value of indicator
 */
@FunctionalInterface
public interface IndicationProvider {
    Long get() throws Exception;
}
