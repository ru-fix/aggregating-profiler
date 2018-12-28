package ru.fix.aggregating.profiler.graphite

import mu.KotlinLogging
import ru.fix.aggregating.profiler.Identity
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.graphite.client.GraphiteEntity
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter
import java.util.*


private val log = KotlinLogging.logger {}

/**
 * Writes single profiler report to graphite
 *
 * @param metricPrefix will be added as a prefix to each metrics before sending them to graphite
 */
class GraphiteReportWriter(
        private val metricPrefix: String,
        private val graphiteWriter: GraphiteWriter) {

    companion object {
        private const val INDICATOR_SUFFIX_MAX = ".indicatorMax"
        private const val INDICATOR_SUFFIX_MIN = ".indicatorMin"
    }

    fun saveProfilingReportToGraphite(report: ProfilerReport) {
        val metrics = convertReportToGraphiteEntity(report)

        graphiteWriter.write(metricPrefix, metrics)
    }

    private fun convertIdentityToMetricName(identity: Identity): String {
        val name = identity.name.replace(' ', '.')

        if (identity.tags.isNotEmpty()) {

            return "$name." + identity.tags.asSequence()
                    .sortedBy { tag -> tag.key }
                    .map { tag -> "${tag.key}.${tag.value}" }
                    .joinToString(separator = ".")

        }
        return name
    }

    private fun convertReportToGraphiteEntity(report: ProfilerReport): List<GraphiteEntity> {
        val metrics = ArrayList<GraphiteEntity>()

        val curTime = System.currentTimeMillis() / 1000
        report.indicators.forEach { key, value ->

            val indicatorName = convertIdentityToMetricName(key)

            listOf(INDICATOR_SUFFIX_MAX, INDICATOR_SUFFIX_MIN).forEach { suffix ->
                metrics.add(
                        GraphiteEntity(
                                indicatorName + suffix,
                                "$value",
                                curTime
                        )
                )
            }
        }

        for (profilerCallReport in report.profilerCallReports) {
            val callName = convertIdentityToMetricName(profilerCallReport.identity)



            profilerCallReport.asMap().forEach { metricName, value ->

                metrics.add(GraphiteEntity(
                        callName + '.'.toString() + metricName,
                        value.toString(),
                        curTime
                ))

            }
        }

        return metrics
    }
}