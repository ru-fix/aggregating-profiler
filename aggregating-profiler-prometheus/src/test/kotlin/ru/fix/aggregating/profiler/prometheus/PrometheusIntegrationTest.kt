package ru.fix.aggregating.profiler.prometheus

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ru.fix.aggregating.profiler.AggregatingProfiler
import ru.fix.aggregating.profiler.Identity
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger { }

class Prometheus(configFile: File) : GenericContainer<Prometheus>("prom/prometheus") {
    init {
        withExposedPorts(9090)
        withFileSystemBind(
                configFile.absolutePath,
                "/etc/prometheus/prometheus.yml",
                BindMode.READ_ONLY)
        withLogConsumer(Slf4jLogConsumer(log))
    }

    val url get() = "http://${this.containerIpAddress}:${this.getMappedPort(9090)}/"
    val hostHost get() = containerInfo.networkSettings.networks.values.first().gateway
}

interface PrometheusApi {
    @POST("-/reload")
    fun reload(): Call<ResponseBody>

    @GET("api/v1/query")
    fun query(@Query("query") query: String): Call<ResponseBody>
}


class PrometheusIntegrationTest {

    @Test
    fun `prometheus polls metrics and user queries them`() {
        val profiler = AggregatingProfiler()

        profiler.attachIndicator("testMetric") { 43L }

        val reporter = profiler.createReporter()
        val prometheusReporter = PrometheusMetricsReporter(reporter)

        val MOCK_METRICS = "metrics"

        val metricsRequestedByPrometheusFlag = AtomicBoolean()

        val endpont = WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .dynamicPort()
                        .extensions(object : ResponseTransformer() {
                            override fun getName() = MOCK_METRICS

                            override fun transform(request: Request?, response: Response?, files: FileSource?, parameters: Parameters?): Response {
                                metricsRequestedByPrometheusFlag.set(true)

                                profiler.profiledCall(Identity("myCall", mapOf("type" to "hard"))).call()

                                return Response.Builder.like(response)
                                        .but().body(prometheusReporter.buildReportAndReset())
                                        .build()
                            }
                        })
        ).apply {
            stubFor(
                    get(urlEqualTo("/metrics"))
                            .willReturn(aResponse()
                                    .withHeader("Content-Type", PrometheusMetricsReporter.CONTENT_TYPE)
                                    .withStatus(200)
                                    .withTransformers(MOCK_METRICS))
            )
        }

        endpont.start()
        println("Metrics endpoint awaits at: http://localhost:${endpont.port()}/metrics")

        val prometheusConfig = File.createTempFile(PrometheusIntegrationTest::class.qualifiedName, ".prometheus.yml").apply {
            deleteOnExit()
            writeText("""
                |global:
                |  scrape_interval: 1s
                """.trimMargin())
        }.absoluteFile

        val prometheus = Prometheus(prometheusConfig)
        prometheus.start()
        println("Prometheus available at: ${prometheus.url}")

        prometheusConfig.writeText("""
            |global:
            |  scrape_interval: 1s
            |scrape_configs:
            |- job_name: integrationTest
            |  static_configs:
            |  - targets: ['${prometheus.hostHost}:${endpont.port()}']
        """.trimMargin())

        val prometheusPid = prometheus.execInContainer("pgrep", "prometheus").stdout.trim().toInt()
        val prometheusHupOutput = prometheus.execInContainer("kill", "-HUP", prometheusPid.toString()).run { "$stdout\n$stderr" }
        println(prometheusHupOutput)


        await("Metrics requested by prometheus")
                .timeout(Duration.FIVE_MINUTES)
                .untilAtomic(metricsRequestedByPrometheusFlag, Matchers.equalTo(true))


        val retrofit = Retrofit.Builder()
                .client(
                        OkHttpClient.Builder()
                                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                                .build()
                )
                .baseUrl(prometheus.url)
                .build()

        val prometheusApi = retrofit.create(PrometheusApi::class.java)

        await("Metrics returned from prometheus")
                .timeout(Duration.FIVE_MINUTES)
                .until({ prometheusApi.query("testMetric").execute().body()!!.string() }, Matchers.containsString("43"))

        await("Metrics returned from prometheus")
                .timeout(Duration.FIVE_MINUTES)
                .until({ prometheusApi.query("myCall_stopSum").execute().body()!!.string() },
                        Matchers.allOf(
                                Matchers.containsString("myCall_stopSum"),
                                Matchers.containsString("type"),
                                Matchers.containsString("hard"),
                                Matchers.containsString("1")
                        )
                )

        endpont.stop()
        prometheus.stop()
    }
}