package ru.fix.commons.profiler.impl;

import org.junit.jupiter.api.Test;
import ru.fix.commons.profiler.PrefixedProfiler;
import ru.fix.commons.profiler.Profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuffixedProfilerTest {

    @Test
    public void whenCreatesProfiledCallThenProfiledCallContainsTrueNameChain() {

        Profiler sp = new SimpleProfiler();
        Profiler root = new SuffixedProfiler(sp, "root");
        Profiler node1 = new SuffixedProfiler(root, "node1");

        assertEquals("root.node1.some_metric", node1.profiledCall("some_metric").toString());
    }

    @Test
    public void whenCreatesProfiledCallWithDotNameThenProfiledCallContainsTrueNameChain() {

        Profiler sp = new SimpleProfiler();
        Profiler root = new SuffixedProfiler(sp, "root.");
        Profiler node1 = new SuffixedProfiler(root, ".node1.");

        assertEquals("root.node1.some_metric", node1.profiledCall("some_metric").toString());
    }

    @Test
    public void whenUsePrefixedProfilerThenProfiledCallContainsTrueNameChain() {

        PrefixedProfiler srv1 = new PrefixedProfiler(new SimpleProfiler(), "srv1.");
        Profiler root = new SuffixedProfiler(srv1, "root");
        Profiler node1 = new SuffixedProfiler(root, "node1");

        assertEquals("srv1.root.node1.some_metric", node1.profiledCall("some_metric").toString());

    }

}