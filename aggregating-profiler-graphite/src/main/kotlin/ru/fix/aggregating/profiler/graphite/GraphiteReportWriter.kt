package ru.fix.aggregating.profiler.graphite

import mu.KotlinLogging
import ru.fix.aggregating.profiler.ProfiledCallReport
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.graphite.client.GraphiteEntity
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter
import java.beans.Introspector
import java.lang.reflect.Method
import java.util.*


private val log = KotlinLogging.logger {}

/**
 * @param metricPrefix will be added as a prefix to each metrics before sending them to graphite
 */
class GraphiteReportWriter(
        private val metricPrefix: String,
        private val graphiteWriter: GraphiteWriter) {

    private val reportFieldExtractor = HashMap<String, Method>()

    init {
        initReportFieldExtractors()
    }


    private fun initReportFieldExtractors() {
        try {
            val propertyDescriptors = Introspector.getBeanInfo(ProfiledCallReport::class.java).propertyDescriptors
            for (descriptor in propertyDescriptors) {
                if (descriptor.propertyType.isPrimitive) {
                    reportFieldExtractor[descriptor.name] = descriptor.readMethod
                }
            }
        } catch (exc: Exception) {
            log.error(exc) { "Failed to initialize field getters for ${ProfiledCallReport::class.qualifiedName} ." }
        }
    }


    fun saveProfilingReportToGraphite(report: ProfilerReport) {
        val metrics = convertReportToGraphiteEntity(report)

        graphiteWriter.write(metricPrefix, metrics)
    }

    private fun convertReportToGraphiteEntity(report: ProfilerReport): List<GraphiteEntity> {
        val metrics = ArrayList<GraphiteEntity>()

        val curTime = System.currentTimeMillis() / 1000
        report.indicators.forEach { key, value ->
            metrics.add(
                    GraphiteEntity(
                            key.replace(' ', '.'),
                            "$value",
                            curTime
                    )
            )
        }

        for (profilerCallReport in report.profilerCallReports) {
            val reportName = profilerCallReport.name


            reportFieldExtractor.forEach { metric, fieldExtractor ->
                var value: Any? = null
                try {
                    value = fieldExtractor.invoke(profilerCallReport)
                } catch (exc: Exception) {
                    log.error(exc) { "Failed to extract report field $metric from $profilerCallReport" }
                }

                if (value is Number) {
                    metrics.add(GraphiteEntity(
                            reportName + '.'.toString() + metric,
                            value.toString(),
                            curTime
                    ))
                }
            }
        }

        return metrics
    }
}