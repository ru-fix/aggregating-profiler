import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import java.net.URI
import ru.fix.gradle.release.plugin.release.ReleaseExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.artifacts.dsl.*
import org.gradle.kotlin.dsl.extra
import org.gradle.api.publication.maven.internal.action.MavenInstallAction
import org.gradle.internal.authentication.DefaultBasicAuthentication
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val groupId = "ru.fix"

buildscript {

    repositories {
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath(Libs.gradleReleasePlugin)
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}")
    }
}

repositories {
    jcenter()
    gradlePluginPortal()
    mavenCentral()
}

plugins {
    kotlin("jvm") version "${Vers.kotlin}" apply false
    signing
}

apply {
    plugin("release")
}


val repositoryUser: String? by project
val repositoryPassword: String? by project
val repositoryUrl: String? by project


subprojects {
    group = "ru.fix"

    apply {
        plugin("maven")
        plugin("signing")
        plugin("java")
    }

    repositories {
        mavenCentral()
        jcenter()
    }


    val sourcesJar by tasks.creating(Jar::class) {
        classifier = "sources"
        from("src/main/java")
        from("src/main/kotlin")
    }

    val javadocJar by tasks.creating(Jar::class) {
        classifier = "javadoc"

        val javadoc = tasks.getByPath("javadoc") as Javadoc
        from(javadoc.destinationDir)
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }

    configure<SigningExtension> {
        sign(configurations.archives)
    }

    tasks {

        "uploadArchives"(Upload::class) {

            dependsOn(javadocJar, sourcesJar)

            repositories {
                withConvention(MavenRepositoryHandlerConvention::class) {
                    mavenDeployer {

                        withGroovyBuilder {
                            //Sign pom.xml file
                            "beforeDeployment" {
                                signing.signPom(delegate as MavenDeployment)
                            }

                            "repository"(
                                    "url" to URI("$repositoryUrl")) {
                                "authentication"(
                                        "userName" to "$repositoryUser",
                                        "password" to "$repositoryPassword"
                                )
                            }
                        }

                        pom.project {
                            withGroovyBuilder {
                                "artifactId"("${project.name}")
                                "groupId"("$groupId")
                                "version"("$version")

                                "name"("${groupId}:${project.name}")
                                "description"("Commons Profiler provide basic API" +
                                        " for application metrics measurement.")

                                "url"("https://github.com/ru-fix/commons-profiler")

                                "licenses" {
                                    "license" {
                                        "name"("The Apache License, Version 2.0")
                                        "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                    }
                                }


                                "developers" {
                                    "developer"{
                                        "id"("gbelyaev")
                                        "name"("Gleb Belyaev")
                                        "url"("https://github.com/gbelyaev")
                                    }
                                    "developer"{
                                        "id"("swarmshine")
                                        "name"("Kamil Asfandiyarov")
                                        "url"("https://github.com/swarmshine")
                                    }
                                }
                                "scm" {
                                    "url"("https://github.com/ru-fix/commons-profiler")
                                    "connection"("https://github.com/ru-fix/commons-profiler.git")
                                    "developerConnection"("https://github.com/ru-fix/commons-profiler.git")
                                }
                            }
                        }
                    }
                }
            }
        }

        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}
