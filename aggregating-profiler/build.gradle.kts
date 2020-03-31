plugins {
    java
    kotlin("jvm")
}

dependencies {
    /**
     * Runtime
     */
    implementation(Libs.slf4j_api)
    implementation(Libs.javax_annotation_jsr305)

    /**
     * Tests
     */

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)

    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.kotlinx_coroutines)

    testImplementation(Libs.slf4j_simple)
    testImplementation(Libs.hamcrest)
}


