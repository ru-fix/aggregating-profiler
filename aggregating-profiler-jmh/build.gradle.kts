import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.champeau.gradle.JMHPluginExtension
import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
//    id("com.github.johnrengelman.shadow")
    id("me.champeau.gradle.jmh")
}

//apply{
//    plugin("me.champeau.gradle.jmh")
//}

jmh{
    warmupIterations = 1
    fork = 1
    duplicateClassesStrategy  = DuplicatesStrategy.WARN
}



//val shadowJar: ShadowJar by tasks
//shadowJar.apply {
//    manifest.attributes["Main-Class"] = "ru.fix.aggregating.profiler.jmh.MainKt"
//}

//    mainClassName = "ru.fix.aggregating.profiler.jmh.SharedCountersJmh"

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


