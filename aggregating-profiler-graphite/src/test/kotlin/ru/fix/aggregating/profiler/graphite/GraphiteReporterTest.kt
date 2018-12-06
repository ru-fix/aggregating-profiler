package ru.fix.aggregating.profiler.graphite

import org.junit.jupiter.api.*
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

class Graphite : GenericContainer<KGenericContainer>("graphiteapp/graphite-statsd") {
    init {
        withExposedPorts(80, 2003)
        waitingFor(Wait.forListeningPort())
    }

    val url get() = "http://${this.containerIpAddress}:${this.getMappedPort(80)}/"
    val writePort get() = this.getMappedPort(2003)
    val writeHost get() = this.containerIpAddress
}

class Grafana : GenericContainer<KGenericContainer>("grafana/grafana") {
    init {
        withExposedPorts(3000)
        waitingFor(Wait.forListeningPort())
    }

    val url get() = "http://${this.containerIpAddress}:${this.getMappedPort(3000)}/"
}


class GraphiteReporterTest {

    val graphite = Graphite()
    val grafana = Grafana()


    @BeforeEach
    fun before() {
        graphite.start()
        println("Visit graphite at:\n${graphite.url}\n" +
                "Write metrics to: ${graphite.writeHost}:${graphite.writePort}")

        grafana.start()
        println("Visit grafana at:\n ${grafana.url}"
        )
    }

    @AfterEach
    fun after() {
        graphite.stop()
        grafana.stop()
    }

    @Test
    fun `hello graphite`() {


    }
}