package ru.fix.commons.profiler.impl;

import org.junit.Assert;
import org.junit.Test;
import ru.fix.commons.profiler.PrefixedProfiler;
import ru.fix.commons.profiler.Profiler;

import static org.junit.Assert.*;

public class NamedProfilerTest {

    @Test
    public void whenCreatesProfiledCallThenProfiledCallContainsTrueNameChain() {

        Profiler sp = new SimpleProfiler();
        Profiler root = new NamedProfiler(sp, "root");
        Profiler node1 = new NamedProfiler(root, "node1");

        Assert.assertEquals("root.node1.some_metric", node1.profiledCall("some_metric").toString());
    }

    @Test
    public void whenUsePrefixedProfilerThenProfiledCallContainsTrueNameChain() {

        PrefixedProfiler srv1 = new PrefixedProfiler(new SimpleProfiler(), "srv1.");
        Profiler root = new NamedProfiler(srv1, "root");
        Profiler node1 = new NamedProfiler(root, "node1");

        Assert.assertEquals("srv1.root.node1.some_metric", node1.profiledCall("some_metric").toString());

    }

}