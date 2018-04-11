package ru.fix.commons.profiler.impl;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class TrimmedDotsStringTest {

    @Test
    public void stringWithoutDotsDoesNotChanges() {
        Assert.assertEquals("s", new TrimmedDotsString("s").toString());
    }

    @Test
    public void ifStringEndsByDotThenDotRemove() {
        Assert.assertEquals("s", new TrimmedDotsString("s.").toString());
    }

    @Test
    public void ifStringStartsByDotThenDotRemove() {
        Assert.assertEquals("s", new TrimmedDotsString(".s").toString());
    }

    @Test
    public void ifStringStartsByDotAndEndsByDotThenDotRemove() {
        Assert.assertEquals("s", new TrimmedDotsString(".s.").toString());
    }

    @Test
    public void ifStringHasBlanksThenBlanksWillBeRemoved() {
        Assert.assertEquals("s", new TrimmedDotsString(" s ").toString());
    }
}