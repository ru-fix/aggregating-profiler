package ru.fix.aggregating.profiler;

import org.junit.jupiter.api.Test;

public class TagsTest {
    @Test
    public void simple_tags() {

        AggregatingProfiler profiler = new AggregatingProfiler();
        ProfiledCall call = profiler.profiledCall("action")
                .tag("filial", "12")
                .tag("serviceId", "4")
                .start();

        call.stop();

        //TODO: remove redundant methods from profiler that copy behavior of profiledCall
        profiler.createReporter().buildReportAndReset();
    }
}
