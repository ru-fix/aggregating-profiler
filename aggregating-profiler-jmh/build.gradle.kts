plugins {
    java
    kotlin("jvm")
    id("me.champeau.gradle.jmh")
}


jmh{
    warmupIterations = 1
    fork = 1
    threads = 8
    duplicateClassesStrategy  = DuplicatesStrategy.WARN

    include.add(".*DifferentWaysToIncrementBatchOfCountersJmh.*")
}


dependencies {
    /**
     * Runtime
     */
    implementation(Libs.slf4j_api)
    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)
    implementation(Libs.slf4j_simple)
    implementation(Libs.jmh)
    implementation(Libs.jmhGeneratorAnn)
    implementation(Libs.jmhGeneratorBytecode)

    implementation(project(":aggregating-profiler"))
}


