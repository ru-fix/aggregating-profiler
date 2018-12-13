package ru.fix.aggregating.profiler.graphite.client


data class GraphiteEntity(
        val name: String,
        val value: String,
        val timestampSec: Long)
