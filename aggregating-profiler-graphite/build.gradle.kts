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
    implementation(Libs.dynamic_property_api)
    implementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix")
    }

    /**
     * Tests
     */

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)



    testImplementation(Libs.slf4j_simple)
    testImplementation(Libs.hamcrest)

    testImplementation(Libs.testcontainers_core)

    testImplementation(Libs.retrofit)
    testImplementation(Libs.okhttp_logging)
    testImplementation(Libs.hamcrest)
    testImplementation(Libs.awaitility)

    testImplementation(Libs.mockk)


}


