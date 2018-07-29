package ru.fix.aggregating.profiler.engine;

import org.junit.jupiter.api.Test;
import ru.fix.aggregating.profiler.PrefixedProfiler;
import ru.fix.aggregating.profiler.Profiler;
import ru.fix.aggregating.profiler.AggregatingProfiler;
import ru.fix.aggregating.profiler.SuffixedProfiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuffixedProfilerTest {

    @Test
    void whenCreatesProfiledCallThenProfiledCallContainsTrueNameChain() {

        Profiler sp = new AggregatingProfiler();
        Profiler root = new SuffixedProfiler(sp, "root");
        Profiler node1 = new SuffixedProfiler(root, "node1");

        assertEquals("root.node1.some_metric", node1.profiledCall("some_metric").toString());
    }

    @Test
    void whenCreatesProfiledCallWithDotNameThenProfiledCallContainsTrueNameChain() {

        Profiler sp = new AggregatingProfiler();
        Profiler root = new SuffixedProfiler(sp, "root.");
        Profiler node1 = new SuffixedProfiler(root, ".node1.");

        assertEquals("root.node1.some_metric", node1.profiledCall("some_metric").toString());
    }

    @Test
    void whenUsePrefixedProfilerThenProfiledCallContainsTrueNameChain() {

        PrefixedProfiler srv1 = new PrefixedProfiler(new AggregatingProfiler(), "srv1.");
        Profiler root = new SuffixedProfiler(srv1, "root");
        Profiler node1 = new SuffixedProfiler(root, "node1");

        assertEquals("srv1.root.node1.some_metric", node1.profiledCall("some_metric").toString());

    }

}