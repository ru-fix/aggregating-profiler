package ru.fix.aggregating.profiler.graphite.client


/**
 * Created by mocichenko on 22.07.2016.
 */
data class GraphiteSettings(
        val port: Int = 0,
        val host: String = "",
        val batchSize: Int = 500,
        val protocol: ProtocolType = ProtocolType.TCP
)
