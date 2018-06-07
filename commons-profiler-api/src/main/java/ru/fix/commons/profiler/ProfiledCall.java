package ru.fix.commons.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public interface ProfiledCall extends AutoCloseable {

    /**
     * If you want to evaluate calls count or whether the method was called
     */
    void call();

    /**
     * if you want to know some metrics then you should start and stop profiled call
     *
     * @throws IllegalStateException if method start called twice
     */
    ProfiledCall start();

    /**
     * Same as stop(1)
     *
     * @see #stop(long)
     */
    void stop();

    /**
     * Call if profiled code executed normally. Applies all measurement to overall metrics.
     */
    void stop(long payload);

    /**
     * Same as start().stop(payload)
     */
    default void startStop(long payload) {
        start().stop(payload);
    }

    /**
     * Call if profiled code didn't execute normally and it's measurements must be discarded.
     * Useful if profiled code failed fast and must not be displayed in latency metrics because it will throw it off.
     */
    void cancel();

    // override `close` interface method which doesn't throw Exception
    @Override
    void close();

    /**
     * Whether call has ended (stopped or cancelled) or not
     */
    boolean isStopped();
}
