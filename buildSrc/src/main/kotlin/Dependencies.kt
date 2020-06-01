object Vers {
    val asciidoctor = "1.5.9.2"
    val kotlin = "1.3.41"
    val kotlin_logging = "1.7.9"
    val kotlinx_coroutines = "1.3.3"
    val sl4j = "1.7.25"
    val dokkav = "0.9.18"
    val gradle_release_plugin = "1.3.17"
    val javax_annotation_jsr305 = "3.0.2"
    val junit = "5.2.0"
    val kotlintest = "3.4.2"
    val jmh = "1.23"

}

object Libs {
    //Plugins
    val gradle_release_plugin = "ru.fix:gradle-release-plugin:${Vers.gradle_release_plugin}"
    val dokka_gradle_plugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}"
    val asciidoctor = "org.asciidoctor:asciidoctor-gradle-plugin:${Vers.asciidoctor}"
    val nexus_staging_plugin = "io.codearte.nexus-staging"
    val nexus_publish_plugin = "de.marcphilipp.nexus-publish"
    val jmh_gradle_plugin = "me.champeau.gradle:jmh-gradle-plugin:0.5.0"

    //Dependencies
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"
    val kotlinx_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Vers.kotlinx_coroutines}"

    val javax_annotation_jsr305 = "com.google.code.findbugs:jsr305:${Vers.javax_annotation_jsr305}"

    val slf4j_api = "org.slf4j:slf4j-api:${Vers.sl4j}"
    val slf4j_simple = "org.slf4j:slf4j-simple:${Vers.sl4j}"

    val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"

    val kotlin_logging = "io.github.microutils:kotlin-logging:${Vers.kotlin_logging}"
    val hamcrest = "org.hamcrest:hamcrest-all:1.3"


    val jmh = "org.openjdk.jmh:jmh-core:${Vers.jmh}"
    val jmhGeneratorAnn = "org.openjdk.jmh:jmh-generator-annprocess:${Vers.jmh}"
    val jmhGeneratorBytecode = "org.openjdk.jmh:jmh-generator-bytecode:${Vers.jmh}"
    val shadowPlugin = "com.github.jengelman.gradle.plugins:shadow:2.0.4"

    val testcontainers_core = "org.testcontainers:testcontainers:1.13.0"

    val retrofit = "com.squareup.retrofit2:retrofit:2.5.0"
    val okhttp_logging = "com.squareup.okhttp3:logging-interceptor:3.12.0"
    val awaitility = "org.awaitility:awaitility:3.1.4"

    val dynamic_property_api = "ru.fix:dynamic-property-api:2.0.4"
    val jfix_stdlib_concurrency = "ru.fix:jfix-stdlib-concurrency:3.0.2"

    val wiremock = "com.github.tomakehurst:wiremock:2.19.0"

    val mockk = "io.mockk:mockk:1.8.13"

    val kotlintest = "io.kotlintest:kotlintest-runner-junit5:${Vers.kotlintest}"
}

enum class Projs {
    `aggregating-profiler`,
    `aggregating-profiler-graphite`,
    `aggregating-profiler-kotlin`,
    `aggregating-profiler-jmh`,
    `aggregating-profiler-prometheus`;
}
