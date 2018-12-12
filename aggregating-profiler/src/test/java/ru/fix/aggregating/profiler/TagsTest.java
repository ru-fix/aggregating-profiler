package ru.fix.aggregating.profiler;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class TagsTest {
    @Test
    public void simple_tags() {

        AggregatingProfiler profiler = new AggregatingProfiler();

        ProfiledCall call = profiler.profiledCall(
                new Identity(
                        "name",
                        "filial", "12",
                        "serviceId", "4",
                        "another", "regular")
        );
        call.stop();

        HashMap<String, String> tags = new HashMap<>();
        tags.put("filial", "12");
        tags.put("filial", "12");

        ProfiledCall call2 = profiler.profiledCall(
                new Identity(
                        "name",

                        "filial", "12",
                        "serviceId", "4",
                        "another", "regular")
        );
        call.stop();

        //TODO: remove redundant methods from profiler that copy behavior of profiledCall
        profiler.createReporter().buildReportAndReset();
    }
}
