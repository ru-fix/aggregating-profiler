import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.champeau.gradle.JMHPluginExtension
import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
    id("me.champeau.gradle.jmh")
}


jmh{
    warmupIterations = 1
    fork = 1
    threads = 4
    duplicateClassesStrategy  = DuplicatesStrategy.WARN
//    include = listOf("DifferentWaysToIncrementBatchOfCountersJmh")
//    include = listOf("CallAggregateJmh.*")


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


