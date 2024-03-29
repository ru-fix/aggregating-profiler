plugins {
    java
    kotlin("jvm")
}

dependencies {
    /**
     * Runtime
     */
    api(project(":aggregating-profiler"))

    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)


    /**
     * Tests
     */

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)


    testImplementation(Libs.slf4j_simple)
    testImplementation(Libs.kotlin_logging)
    testImplementation(Libs.hamcrest)

    testImplementation(Libs.testcontainers_core)

    testImplementation(Libs.retrofit)
    testImplementation(Libs.okhttp_logging)
    testImplementation(Libs.awaitility)

    testImplementation(Libs.wiremock)

}


