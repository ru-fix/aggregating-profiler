package ru.fix.aggregating.profiler.graphite

import mu.KotlinLogging
import ru.fix.aggregating.profiler.Profiler
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.RegexpTagger
import ru.fix.aggregating.profiler.Tagger
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter
import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.stdlib.concurrency.threads.NamedExecutors
import ru.fix.stdlib.concurrency.threads.ReschedulableScheduler
import ru.fix.stdlib.concurrency.threads.Schedule

private val log = KotlinLogging.logger {}

/**
 * Regularly builds and writes profiler reports to graphite
 *
 * @param metricPrefix will be added to each metric.
 *      Usually contains server instance id.
 *      E.g. `profiler.web1` will lead metric `http.requests.callsCountSum` to be written
 *      as `profiler.web1.http.requests.callsCountSum`
 */
class GraphiteProfilerReporter(
        private val profiler: Profiler,
        private val settings: DynamicProperty<GraphiteProfilerReporterSettings>,
        private val metricPrefix: String) : AutoCloseable {


    companion object {
        private const val GRAPHITE_RATE_TAG = "logRate"
        private const val THREAD_NAME_PREFIX = "graphite-profiler-reporter-"
    }


    private val profilerReporter = profiler.createReporter()
    private val graphiteWriter = GraphiteWriter()
    private val graphiteReportWriter = GraphiteReportWriter(metricPrefix, graphiteWriter)

    private val scheduler = mutableListOf<ReschedulableScheduler>()


    init {
        settings.addListener(this::onSettingsChanged)
        onSettingsChanged(settings.get())
    }

    private fun onSettingsChanged(settings: GraphiteProfilerReporterSettings) {
        try {
            graphiteWriter.connect(settings.graphiteSettings)
        } catch (e: Exception) {
            log.error(e) { "Couldn't open connection to graphite. Settings: $settings" }
        }


        scheduleReporting(settings.selectiveRateProfilingConfig)
    }

    private fun scheduleReporting(config: SelectiveRateProfilingConfig) {
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

        this.profiler.setTagger(RegexpTagger(GRAPHITE_RATE_TAG, plainConf))


        synchronized(scheduler) {
            scheduler.forEach(ReschedulableScheduler::shutdown)
            scheduler.clear()

            plainConf.keys.forEach { key ->
                scheduler.add(
                        makeScheduler(key.toLong()) {
                            buildAndSaveReportInGraphite {
                                profilerReporter.buildReportAndReset(GRAPHITE_RATE_TAG, key)
                            }
                        })
            }

            scheduler.add(
                    makeScheduler(config.defaultTimeout) {
                        buildAndSaveReportInGraphite {
                            profilerReporter.buildReportAndReset(GRAPHITE_RATE_TAG, Tagger.EMPTY_VALUE)
                        }
                    })
        }
    }


    private fun makeScheduler(rate: Long, task: () -> Unit): ReschedulableScheduler {
        val newScheduler = NamedExecutors.newSingleThreadScheduler(THREAD_NAME_PREFIX, profiler)
        newScheduler.schedule({ Schedule.withRate(rate) }, 0L, task)
        return newScheduler
    }

    private fun buildAndSaveReportInGraphite(reporter: () -> ProfilerReport) {
        if (!settings.get().enableGraphiteProfiling) {
            return
        }

        val report = reporter()

        try {
            graphiteReportWriter.saveProfilingReportToGraphite(report)
        } catch (e: Exception) {
            log.error(e) { "Failed send metrics to graphite." }
        }
    }


    override fun close() {
        profilerReporter.close()
    }
}