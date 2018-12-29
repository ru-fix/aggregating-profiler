package ru.fix.aggregating.profiler.prometheus

import ru.fix.aggregating.profiler.Identity
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.ProfilerReporter
import java.io.StringWriter
import java.io.Writer

class PrometheusMetricsReporter(private val reporter: ProfilerReporter): AutoCloseable {

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

        report.indicators.forEach { identity, value ->
            writer.appendGaugeType(identity)
            writer.appendGaugeValue(identity, value.toDouble())
        }

        report.profilerCallReports.forEach { report ->
            report.asMap().forEach { metric, value ->

                val identity = Identity(report.identity.name + "_$metric", report.identity.tags)
                writer.appendGaugeType(identity)
                writer.appendGaugeValue(identity, value.toDouble())
            }
        }
        return writer.toString()
    }

    private fun Writer.appendGaugeType(identity: Identity) {
        this.appendln("# HELP ${normalizeName(identity.name)} ")
        this.appendln("# TYPE ${normalizeName(identity.name)} gauge")
    }

    private fun Writer.appendGaugeValue(identity: Identity, value: Double) {
        this.appendln("${convertIdentityToMetricName(identity)} ${serializeDouble(value)}")
    }

    private fun convertIdentityToMetricName(identity: Identity): String{
        val metricName = normalizeName(identity.name)
        if (identity.tags.isNotEmpty()) {

            return "$metricName" + identity.tags.asSequence()
                    .sortedBy { tag -> tag.key }
                    .map { tag -> """${tag.key}="${escapeTagValue(tag.value)}"""" }
                    .joinToString(separator = ",", prefix = "{", postfix = "}")
        }
        return metricName
    }

    private fun escapeTagValue(value: String): String {
        val result = StringBuilder()

        for (char in value ) {
            when (char) {
                '\\' -> result.append("\\\\")
                '\"' -> result.append("\\\"")
                '\n' -> result.append("\\n")
                else -> result.append(char)
            }
        }
        return result.toString()
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

    override fun close() {
        reporter.close()
    }
}