package ru.fix.commons.profiler.impl;

import org.junit.Assert;
import org.junit.Test;
import ru.fix.commons.profiler.util.NameNormalizer;

public class NameNormalizerTest {

    @Test
    public void stringWithoutDotsDoesNotChanges() {
        Assert.assertEquals("s", NameNormalizer.trimDots("s"));
    }

    @Test
    public void ifStringEndsByDotThenDotRemove() {
        Assert.assertEquals("s", NameNormalizer.trimDots("s."));
    }

    @Test
    public void ifStringStartsByDotThenDotRemove() {
        Assert.assertEquals("s", NameNormalizer.trimDots(".s"));
    }

    @Test
    public void ifStringStartsByDotAndEndsByDotThenDotRemove() {
        Assert.assertEquals("s", NameNormalizer.trimDots(".s."));
    }

    @Test
    public void ifStringHasBlanksThenBlanksWillBeRemoved() {
        Assert.assertEquals("s", NameNormalizer.trimDots(" s "));
    }
}