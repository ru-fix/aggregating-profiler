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
    compile(Libs.slf4j_api)
    compile(Libs.kotlin_jdk8)
    compile(Libs.kotlin_stdlib)
    compile(Libs.kotlin_reflect)
    compile(Libs.slf4j_simple)
    compile(Libs.jmh)
    compile(Libs.jmhGeneratorAnn)
    compile(Libs.jmhGeneratorBytecode)

    compile(project(":aggregating-profiler"))
}


