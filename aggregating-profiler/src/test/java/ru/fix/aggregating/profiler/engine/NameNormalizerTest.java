package ru.fix.aggregating.profiler.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameNormalizerTest {

    @Test
    void stringWithoutDotsDoesNotChanges() {
        assertEquals("s", NameNormalizer.trimDots("s"));
    }

    @Test
    void ifStringEndsByDotThenDotRemove() {
        assertEquals("s", NameNormalizer.trimDots("s."));
    }

    @Test
    void ifStringStartsByDotThenDotRemove() {
        assertEquals("s", NameNormalizer.trimDots(".s"));
    }

    @Test
    void ifStringStartsByDotAndEndsByDotThenDotRemove() {
        assertEquals("s", NameNormalizer.trimDots(".s."));
    }

    @Test
    void ifStringHasBlanksThenBlanksWillBeRemoved() {
        assertEquals("s", NameNormalizer.trimDots(" s "));
    }
}