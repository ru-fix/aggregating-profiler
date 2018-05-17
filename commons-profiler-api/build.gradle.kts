import org.gradle.kotlin.dsl.*


plugins {
    java
}

dependencies {
    /**
     * Runtime
     */
    compile(Libs.slf4j_api)

    /**
     * Tests
     */
    testCompile(Libs.junit)
    testCompile(Libs.mockito)
    testCompile(Libs.slf4j_simple)
    testCompile(Libs.guava)
}


