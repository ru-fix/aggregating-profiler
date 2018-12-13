package ru.fix.aggregating.profiler.graphite

import mu.KotlinLogging
import ru.fix.aggregating.profiler.Profiler
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter
import ru.fix.dynamic.property.api.DynamicProperty

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
        profiler: Profiler,
        settings: DynamicProperty<GraphiteProfilerReporterSettings>,
        private val metricPrefix: String) : AutoCloseable {

    private val selectiveReporter: SelectiveRateReporter

    private val graphiteWriter = GraphiteWriter()
    private val graphiteReportWriter = GraphiteReportWriter(metricPrefix, graphiteWriter)

    init {
        settings.addListener(this::onSettingsChanged)
        onSettingsChanged(settings.get())

        selectiveReporter = SelectiveRateReporter(
                profiler,
                settings.map(GraphiteProfilerReporterSettings::selectiveRateProfilingConfig),
                ::saveReportInGraphite
        )
    }

    private fun onSettingsChanged(settings: GraphiteProfilerReporterSettings) {
        try {
            graphiteWriter.connect(settings.graphiteSettings)
        } catch (e: Exception) {
            log.error(e) { "Couldn't open connection to graphite. Settings: $settings" }
        }
    }


    private fun saveReportInGraphite(report: ProfilerReport) {
        try {
            graphiteReportWriter.saveProfilingReportToGraphite(report)
        } catch (e: Exception) {
            log.error(e) { "Failed send metrics to graphite." }
        }
    }

    override fun close() {
        selectiveReporter.close()
    }
}