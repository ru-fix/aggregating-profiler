package ru.fix.aggregating.profiler;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

class TagsTest {
    @Test
    void simple_tags() {

        AggregatingProfiler profiler = new AggregatingProfiler();

        ProfiledCall call = profiler.profiledCall(
                new Identity(
                        "name",
                        "filial", "12",
                        "serviceId", "13",
                        "another", "regular")
        );
        call.stop();

        HashMap<String, String> tags = new HashMap<>();
        tags.put("filial", "102");
        tags.put("serviceId", "103");

        ProfiledCall call2 = profiler.profiledCall(new Identity("name", tags));
        call2.stop();

        //TODO: remove redundant methods from profiler that copy behavior of profiledCall
        ProfilerReport report = profiler.createReporter().buildReportAndReset();
        System.out.println(report);
    }
}
