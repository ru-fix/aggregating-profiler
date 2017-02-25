package ru.fix.commons.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public interface ProfiledCall {

    /**
     * @throws IllegalStateException if method start called twice
     */
    ProfiledCall start();

    /**
     * Same as stop(1)
     *
     * @throws IllegalStateException if method stop called twice
     * @see #stop(long)
     */
    void stop();

    /**
     * @param payload
     * @throws IllegalStateException if method stop called twice
     */
    void stop(long payload);

    boolean isStopped();
}
