package ru.fix.aggregating.profiler.impl;

import org.junit.jupiter.api.Test;
import ru.fix.aggregating.profiler.util.NameNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameNormalizerTest {

    @Test
    public void stringWithoutDotsDoesNotChanges() {
        assertEquals("s", NameNormalizer.trimDots("s"));
    }

    @Test
    public void ifStringEndsByDotThenDotRemove() {
        assertEquals("s", NameNormalizer.trimDots("s."));
    }

    @Test
    public void ifStringStartsByDotThenDotRemove() {
        assertEquals("s", NameNormalizer.trimDots(".s"));
    }

    @Test
    public void ifStringStartsByDotAndEndsByDotThenDotRemove() {
        assertEquals("s", NameNormalizer.trimDots(".s."));
    }

    @Test
    public void ifStringHasBlanksThenBlanksWillBeRemoved() {
        assertEquals("s", NameNormalizer.trimDots(" s "));
    }
}