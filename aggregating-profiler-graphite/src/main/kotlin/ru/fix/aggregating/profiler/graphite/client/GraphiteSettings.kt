package ru.fix.aggregating.profiler.graphite.client

import lombok.Data


/**
 * Created by mocichenko on 22.07.2016.
 */

@Data
class GraphiteSettings(
        val port: Int,
        val host: String,
        val batchSize: Int,
        val protocol: ProtocolType
)
