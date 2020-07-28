package ru.fix.aggregating.profiler

import io.kotlintest.matchers.maps.shouldContainExactly
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test


class PrefixedProfilerTest {

    @Test
    fun `prefixed profiler adds prefix to name and tags to identity`() {
        val profiler = AggregatingProfiler()
        val prefixedProfiler = PrefixedProfiler(profiler, "prefix", mapOf("prefixTagName" to "prefixTagValue"))

        val reporter = profiler.createReporter()

        prefixedProfiler.profiledCall(Identity("task", "taskTagName", "taskTagValue")).profile {}

        reporter.buildReportAndReset().profilerCallReports.single().apply {
            identity.name.shouldBe("prefix.task")
            identity.tags.shouldContainExactly(mapOf(
                    "prefixTagName" to "prefixTagValue",
                    "taskTagName" to "taskTagValue"
            ))
            stopSum.shouldBe(1)
        }
    }

}