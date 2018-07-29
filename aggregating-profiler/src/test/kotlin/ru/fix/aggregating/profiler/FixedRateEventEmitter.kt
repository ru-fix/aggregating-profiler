package ru.fix.aggregating.profiler

import java.util.concurrent.atomic.AtomicBoolean

class FixedRateEventEmitter(
        val emitsPerSecond: Int,
        val task: () -> Boolean) : AutoCloseable {

    companion object {
        val MIN_EMITS_PER_SECOND = 1
        val MAX_EMITS_PER_SECOND = 1000
    }

    private val thread = Thread(this::run, FixedRateEventEmitter::class.java.name)
    private val shutdownFlag = AtomicBoolean()


    init {
        assertEmitsPerSecond(emitsPerSecond)
        thread.start()
    }

    private fun assertEmitsPerSecond(emitsPerSecond: Int) {
        if (emitsPerSecond < MIN_EMITS_PER_SECOND || emitsPerSecond > MAX_EMITS_PER_SECOND) {
            throw IllegalArgumentException("""
                FixedRateEventEmitter emitsPerSecond is set to $emitsPerSecond.
                Expected value between $MIN_EMITS_PER_SECOND and $MAX_EMITS_PER_SECOND
                """.trimIndent())
        }
    }

    private fun run() {

        var lastTaskInvocationTimesMs = 0L

        while (!Thread.currentThread().isInterrupted && !shutdownFlag.get()) {

            if (lastTaskInvocationTimesMs == 0L) {

                //First launch of task
                if(!task()){
                    shutdownFlag.set(true)
                }
                lastTaskInvocationTimesMs = System.currentTimeMillis()
                continue
            }

            val timestamp = System.currentTimeMillis()
            val spentTimeFromLastInvocation = timestamp - lastTaskInvocationTimesMs

            val minimalDelayBeforeNextInvocation = MAX_EMITS_PER_SECOND / emitsPerSecond

            if (spentTimeFromLastInvocation >= minimalDelayBeforeNextInvocation) {
                if(!task()){
                    shutdownFlag.set(true)
                }
                lastTaskInvocationTimesMs = System.currentTimeMillis()
            } else {
                Thread.sleep(minimalDelayBeforeNextInvocation - spentTimeFromLastInvocation)
            }
        }
    }

    override fun close() {
        shutdownFlag.set(true)
        thread.join()
    }

    fun join() {
        thread.join()
    }
}