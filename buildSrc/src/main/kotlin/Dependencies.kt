object Vers {
    val kotlin = "1.3.10"
    val kotlin_logging = "1.6.22"
    val sl4j = "1.7.25"
    val dokkav = "0.9.16"
    val gradleReleasePlugin = "1.3.3"
    val junit = "5.2.0"
    val jmh = "1.21"

}

object Libs {
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"

    val gradleReleasePlugin = "ru.fix:gradle-release-plugin:${Vers.gradleReleasePlugin}"
    val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}"

    val slf4j_api = "org.slf4j:slf4j-api:${Vers.sl4j}"
    val slf4j_simple = "org.slf4j:slf4j-simple:${Vers.sl4j}"

    val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"

    val kotlin_logging = "io.github.microutils:kotlin-logging:${Vers.kotlin_logging}"
    val hamcrest = "org.hamcrest:hamcrest-all:1.3"

    val jmhGradlePlugin = "me.champeau.gradle:jmh-gradle-plugin:0.4.7"
    val jmh = "org.openjdk.jmh:jmh-core:${Vers.jmh}"
    val jmhGeneratorAnn = "org.openjdk.jmh:jmh-generator-annprocess:${Vers.jmh}"
    val jmhGeneratorBytecode = "org.openjdk.jmh:jmh-generator-bytecode:${Vers.jmh}"
    val shadowPlugin = "com.github.jengelman.gradle.plugins:shadow:2.0.4"

    val testcontainers_core = "org.testcontainers:testcontainers:1.10.2"
    val lombok = "org.projectlombok:lombok:1.18.4"

    val retrofit = "com.squareup.retrofit2:retrofit:2.5.0"
    val okhttp_logging = "com.squareup.okhttp3:logging-interceptor:3.12.0"
    val awaitility = "org.awaitility:awaitility:3.1.4"

    val dynamicPropertyApi = "ru.fix:dynamic-property-api:1.0.5"
    val jfixStdlibConcurrency = "ru.fix:jfix-stdlib-concurrency:1.0.13"
    val wiremock = "com.github.tomakehurst:wiremock:2.19.0"
}


