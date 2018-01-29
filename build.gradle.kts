import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import java.net.URI
import ru.fix.gradle.release.plugin.release.ReleaseExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.artifacts.dsl.*
import org.gradle.kotlin.dsl.extra

buildscript {

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath(Libs.gradleReleasePlugin)
    }
}

plugins {
    base
    kotlin("jvm") version Vers.kotlin apply false
    id("maven-publish")
}

apply {
    plugin("release")
}


val repositoryUser by project
val repositoryPassword by project
val repositoryUrl by project



allprojects {
    group = "ru.fix"

    apply {
        plugin("maven-publish")
    }

    repositories {
        mavenCentral()
        jcenter()
    }


    publishing {
        (publications) {
            if (components.names.contains("java")) {
                logger.info("Register java artifact for project: ${project.name}")

                val sourcesJar by tasks.creating(Jar::class) {
                    classifier = "sources"
                    from("src/main/java")
                    from("src/main/kotlin")
                }

                "${project.name}-mvnPublication"(MavenPublication::class) {
                    from(components["java"])
                    artifact(sourcesJar)
                    pom.withXml {
                        asNode().apply {
                            appendNode("description",
                                    "Commons Profiler provide basic API for application metrics measurement.")
                            appendNode("licenses").appendNode("license").apply {
                                appendNode("name", "The Apache License, Version 2.0")
                                appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                credentials {
                    username = "$repositoryUser"
                    password = "$repositoryPassword"
                }
                name = "remoteRepository"
                url = URI("$repositoryUrl")
            }
        }
    }
}
