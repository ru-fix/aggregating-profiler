import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    /**
     * Runtime
     */
    compile(Libs.slf4j_api)

    compile(project(":aggregating-profiler"))

    compile(Libs.kotlin_jdk8)
    compile(Libs.kotlin_stdlib)
    compile(Libs.kotlin_reflect)

    compile(Libs.kotlin_logging)

    /**
     * Tests
     */

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)


    testCompile(Libs.slf4j_simple)
    testCompile(Libs.hamcrest)

    testCompile(Libs.testcontainers_core)

    testCompile(Libs.retrofit)
    testCompile(Libs.okhttp_logging)
    testCompile(Libs.awaitility)
}


