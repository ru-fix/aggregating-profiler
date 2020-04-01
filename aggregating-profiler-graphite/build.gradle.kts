plugins {
    java
    kotlin("jvm")
}

dependencies {
    /**
     * Runtime
     */
    api(Libs.slf4j_api)
    api(project(":aggregating-profiler"))
    api(Libs.kotlin_logging)
    api(Libs.dynamic_property_api)

    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)


    implementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix")
    }

    /**
     * Tests
     */

    testImplementation(Libs.junit_api)
    testImplementation(Libs.junit_engine)



    testImplementation(Libs.slf4j_simple)
    testImplementation(Libs.hamcrest)

    testImplementation(Libs.testcontainers_core)

    testImplementation(Libs.retrofit)
    testImplementation(Libs.okhttp_logging)
    testImplementation(Libs.hamcrest)
    testImplementation(Libs.awaitility)

    testImplementation(Libs.mockk)

}


