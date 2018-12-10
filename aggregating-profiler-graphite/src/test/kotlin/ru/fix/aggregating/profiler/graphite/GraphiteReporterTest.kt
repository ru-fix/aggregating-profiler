package ru.fix.aggregating.profiler.graphite

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.awaitility.Awaitility.await
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import ru.fix.aggregating.profiler.ProfiledCallReport
import ru.fix.aggregating.profiler.ProfilerReport
import ru.fix.aggregating.profiler.graphite.client.GraphiteSettings
import ru.fix.aggregating.profiler.graphite.client.GraphiteWriter
import ru.fix.aggregating.profiler.graphite.client.ProtocolType

class Graphite : GenericContainer<Graphite>("graphiteapp/graphite-statsd") {
    init {
        withExposedPorts(80, 2003)
        waitingFor(Wait.forListeningPort())
    }

    val url get() = "http://${this.containerIpAddress}:${this.getMappedPort(80)}/"
    val writePort get() = this.getMappedPort(2003)
    val writeHost get() = this.containerIpAddress
}

interface GraphiteApi {
    @GET("render")
    fun query(
            @Query("format") format: String = "raw",
            @Query("from") from: String? = null,
            @Query("until") until: String? = null,
            @Query("target") target: String
    ): Call<ResponseBody>
}

class GraphiteReporterTest {

    val graphite = Graphite()

    @BeforeEach
    fun before() {
        graphite.start()
        println("Visit graphite at:\n${graphite.url}\n" +
                "Write metrics to: ${graphite.writeHost}:${graphite.writePort}")
    }

    @AfterEach
    fun after() {
        graphite.stop()
    }

    @Test
    fun `write profiler report indicators and metrics to graphite`() {

        val graphiteWriter = GraphiteWriter()
        graphiteWriter.connect(GraphiteSettings(
                graphite.writePort,
                graphite.writeHost,
                0,
                ProtocolType.TCP
        ))

        val reportWriter = GraphiteReportWriter("metricPrefix", graphiteWriter)

        val callReport1 = ProfiledCallReport("call1").apply {
            callsCountSum = 107
            reportingTimeAvg = 60_000
        }


        val callReport2 = ProfiledCallReport("call2").apply {
            callsCountSum = 108
            reportingTimeAvg = 60_000
            callsThroughputAvg = 12.25
        }

        val profilerReport = ProfilerReport(
                mapOf(
                        "indicator1" to 12L,
                        "indicator2" to 42L
                ),
                listOf(callReport1, callReport2))

        reportWriter.saveProfilingReportToGraphite(profilerReport)


        val retrofit = Retrofit.Builder()
                .client(
                        OkHttpClient.Builder()
                                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                                .build()
                )
                .baseUrl(graphite.url)
                .build()

        val graphiteApi = retrofit.create(GraphiteApi::class.java)


        await().until(
                {
                    graphiteApi.query(target = "metricPrefix.indicator1", from = "-2minutes")
                            .execute()
                            .body()?.string()
                },
                CoreMatchers.containsString("12.0"))

        await().until(
                {
                    graphiteApi.query(target = "metricPrefix.indicator2", from = "-2minutes")
                            .execute()
                            .body()?.string()
                },
                CoreMatchers.containsString("42.0"))

        await().until(
                {
                    graphiteApi.query(target = "metricPrefix.call1.callsCountSum", from = "-2minutes")
                            .execute()
                            .body()?.string()
                },
                CoreMatchers.containsString("107.0"))

        await().until(
                {
                    graphiteApi.query(target = "metricPrefix.call2.callsThroughputAvg", from = "-2minutes")
                            .execute()
                            .body()?.string()
                },
                CoreMatchers.containsString("12.25"))


        graphiteWriter.close()
    }
}