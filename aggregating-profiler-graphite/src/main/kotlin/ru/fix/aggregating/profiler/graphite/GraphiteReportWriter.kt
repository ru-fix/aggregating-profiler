package ru.fix.aggregating.profiler.graphite

import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import ru.fix.aggregating.profiler.ProfiledCallReport
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.graphite.client.GraphiteEntity
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter
import java.util.*


/**
 * @param metricPrefix will be added for each metrics before sending to graphite
 */
class GraphiteReportWriter(private val metricPrefix: String) {

    val graphiteWriter = GraphiteWriter()

    fun saveProfilingReportInGraphite(report: ProfilerReport) {
        val metrics = convertReportToGraphiteEntity(report)

        graphiteWriter.write(metricPrefix, metrics)

    }

    private fun convertReportToGraphiteEntity(report: ProfilerReport): List<GraphiteEntity> {
        val metrics = ArrayList<GraphiteEntity>()

        val curTime = System.currentTimeMillis() / 1000
//        report.indicators.forEach { key, value ->
//            metrics.add(
//                    GraphiteEntity(
//                            key.replace(' ', '.'),
//                            "$value",
//                            curTime
//                    )
//            )
//        }

        for (profilerCallReport in report.profilerCallReports) {
            val reportName = profilerCallReport.name




//            metrics.add(GraphiteEntity(
//                    "$reportName.${ProfiledCallReport::getActiveCallsCountMax.name}",
//                    profilerCallReport.activeCallsCountMax.toString(),
//                    curTime)),



//
//            reportFieldExtractor.forEach({ metric, fieldExtractor ->
//                var value: Any? = null
//                try {
//                    value = fieldExtractor.invoke(profilerCallReport)
//                } catch (exc: Exception) {
//                    log.error(
//                            "Failed to extract report field {} from object {}",
//                            metric, Marshaller.safeDumpToLog(profilerCallReport),
//                            exc
//                    )
//                }
//
//                if (value is Number) {
//                    metrics.add(GraphiteEntity(
//                            reportName + '.'.toString() + metric,
//                            value.toLong(),
//                            curTime
//                    ))
//                }
//            })
        }

        return metrics
    }
}