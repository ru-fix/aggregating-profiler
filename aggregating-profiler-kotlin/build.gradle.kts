plugins {
    java
    kotlin("jvm")
}

dependencies {
    /**
     * Runtime
     */
    implementation(Libs.slf4j_api)

    implementation(project(":aggregating-profiler"))

    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)

    implementation(Libs.kotlin_logging)

    /**
     * Tests
     */
    testImplementation(Libs.junit_api)
    testImplementation(Libs.kotlintest)
    testImplementation(Libs.mockk)
    testImplementation(Libs.kotlinx_coroutines)
    testRuntimeOnly(Libs.junit_engine)
}
