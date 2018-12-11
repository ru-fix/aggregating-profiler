package ru.fix.aggregating.profiler.prometheus

import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.ProfilerReporter
import java.io.StringWriter
import java.io.Writer

class PrometheusMetricsReporter(private val reporter: ProfilerReporter) {

    companion object {
        const val CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8"
    }

    fun buildReportAndReset(): String {
        val report = reporter.buildReportAndReset()
        return serializeReport(report)
    }

    private fun normalizeName(name: String) = name.replace('.', '_')

    private fun serializeReport(report: ProfilerReport): String {

        val writer = StringWriter()

        report.indicators.forEach { name, value ->
            writer.appendGaugeType(name)
            writer.appendGaugeValue(name, value.toDouble())
        }

        report.profilerCallReports.forEach { report ->
            report.asMap().forEach { key, value ->
                val name = "${report.name}_$key"
                writer.appendGaugeType(name)
                writer.appendGaugeValue(name, value.toDouble())
            }
        }
        return writer.toString()
    }

    private fun Writer.appendGaugeType(name: String) {
        this.appendln("# TYPE ${normalizeName(name)} gauge")
    }

    private fun Writer.appendGaugeValue(name: String, value: Double) {
        this.appendln("${normalizeName(name)} ${serializeDouble(value)}")
    }

    private fun serializeDouble(doubleValue: Double): String {
        if (doubleValue == java.lang.Double.POSITIVE_INFINITY) {
            return "+Inf"
        }
        if (doubleValue == java.lang.Double.NEGATIVE_INFINITY) {
            return "-Inf"
        }
        return if (java.lang.Double.isNaN(doubleValue)) {
            "NaN"
        } else java.lang.Double.toString(doubleValue)
    }

}