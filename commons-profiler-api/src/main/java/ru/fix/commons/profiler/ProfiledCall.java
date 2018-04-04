package ru.fix.commons.profiler;

/**
 * @author Kamil Asfandiyarov
 */
public interface ProfiledCall {

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
     * @throws IllegalStateException if call has been already stopped or cancelled
     * @see #stop(long)
     */
    void stop();

    /**
     * Call if profiled code executed normally. Applies all measurement to overall metrics.
     *
     * @param payload
     * @throws IllegalStateException if call has been already stopped or cancelled
     */
    void stop(long payload);

    /**
     * Call if profiled code didn't execute normally and it's measurements must be discarded.
     * Useful if profiled code failed fast and must not be displayed in latency metrics because it will throw it off.
     *
     * @throws IllegalStateException if call has been already stopped or cancelled
     */
    void cancel();

    /**
     * Whether call has ended (stopped or cancelled) or not
     */
    boolean isStopped();
}
