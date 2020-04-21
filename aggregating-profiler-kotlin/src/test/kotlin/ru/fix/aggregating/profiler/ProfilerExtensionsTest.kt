package ru.fix.aggregating.profiler

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrowAny
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ProfilerExtensionsTest {

    private val noopProfiler = NoopProfiler()

    @Test
    fun `profileInline should support suspend functions`() = runBlocking {
        noopProfiler.profiledCall("test")
            .profileBlock {
                delay(0)
            }

        noopProfiler.profileBlock("test") { delay(0) }
        noopProfiler.profileBlock(Identity("test")) { delay(0) }
    }

    @Test
    fun `profileInline should close on exception`() {
        val call = spyk(noopProfiler.profiledCall("test"))

        shouldThrowAny {
            call.profileBlock { error("test error") }
        }

        verify {
            call.close()
        }
    }

    @Test
    fun `profileInline should support both Runnable and Supplier`() {
        noopProfiler.profileBlock("test") { 1 } shouldBe 1
        noopProfiler.profileBlock("test") { doSomething() }
    }

    private fun doSomething() {
        //empty
    }
}