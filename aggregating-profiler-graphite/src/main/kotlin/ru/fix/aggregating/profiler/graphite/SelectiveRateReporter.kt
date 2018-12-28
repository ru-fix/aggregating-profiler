package ru.fix.aggregating.profiler.graphite

import mu.KotlinLogging
import ru.fix.aggregating.profiler.Profiler
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.RegexpLabelSticker
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.stdlib.concurrency.threads.NamedExecutors
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler
import ru.fix.stdlib.concurrency.threads.Schedule

private val log = KotlinLogging.logger {}

/**
 * Regularly builds and writes profiler reports to external data source
 */
class SelectiveRateReporter(
        private val profiler: Profiler,
        private val settings: DynamicProperty<SelectiveRateProfilingConfig>,
        private val storeMetrics: (ProfilerReport) -> Unit) : AutoCloseable {

    companion object {
        private const val THREAD_NAME_PREFIX = "profiler-selective-reporter"
        private const val RATE_LABEL = "logRate"
    }

    private val profilerReporter = profiler.createReporter()

    private val scheduler = mutableListOf<ReschedulableScheduler>()

    init {
        settings.addListener(this::reScheduleReporting)
        reScheduleReporting(settings.get())
    }

    private fun reScheduleReporting(config: SelectiveRateProfilingConfig) {
        if (config.reportingSchedule.isEmpty()) {
            synchronized(scheduler) {
                scheduler.forEach(ReschedulableScheduler::shutdown)
                scheduler.clear()
                scheduler.add(
                        makeScheduler(config.defaultTimeout) {
                            buildAndSaveReportInGraphite { profilerReporter.buildReportAndReset() }
                        }
                )
            }
            return
        }

        val plainConf = config
                .reportingSchedule
                .entries
                .map { entry ->
                    entry.key.toString() to entry.value.map { java.util.regex.Pattern.compile(it) }.toSet()
                }
                .toMap()

        this.profiler.setLabelSticker(RegexpLabelSticker(RATE_LABEL, plainConf))


        synchronized(scheduler) {
            scheduler.forEach(ReschedulableScheduler::shutdown)
            scheduler.clear()

            plainConf.keys.forEach { key ->
                scheduler.add(
                        makeScheduler(key.toLong()) {
                            buildAndSaveReportInGraphite {
                                profilerReporter.buildReportAndReset { _, labels ->
                                    labels[RATE_LABEL]?.let { labelValue -> labelValue == key } ?: false
                                }
                            }
                        })
            }

            scheduler.add(
                    makeScheduler(config.defaultTimeout) {
                        buildAndSaveReportInGraphite {
                            profilerReporter.buildReportAndReset { _, labels ->
                                !labels.containsKey(RATE_LABEL)
                            }
                        }
                    })
        }
    }


    private fun makeScheduler(rate: Long, task: () -> Unit): ReschedulableScheduler {
        val newScheduler = NamedExecutors.newSingleThreadScheduler(THREAD_NAME_PREFIX, profiler)
        newScheduler.schedule({ Schedule.withRate(rate) }, 0L, task)
        return newScheduler
    }

    private fun buildAndSaveReportInGraphite(buildReport: () -> ProfilerReport) {
        val report = buildReport()

        if (settings.get().enableReporting) {
            try {
                storeMetrics(report)
            } catch (e: Exception) {
                log.error(e) { "Failed send metrics to graphite." }
            }
        }
    }


    override fun close() {
        synchronized(scheduler) {
            scheduler.forEach { it.shutdown() }
        }
        profilerReporter.close()
    }
}