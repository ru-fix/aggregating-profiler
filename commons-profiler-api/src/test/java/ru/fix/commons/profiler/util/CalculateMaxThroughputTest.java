package ru.fix.commons.profiler.util;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Gleb Beliaev (gbeliaev@fix.ru)
 * Created 11.01.18.
 */
public class CalculateMaxThroughputTest {

    @Test
    public void name() throws Exception {
        CalculateMaxThroughput c = spy(new CalculateMaxThroughput());

        when(c.currentTimeMillis()).thenReturn(1000L, 1100L, 2000L);
        c.call();
        c.call();
        c.call();
        assertEquals(2, c.getMaxAndReset());

        when(c.currentTimeMillis()).thenReturn(3000L, 4000L, 4100L, 4200L, 5000L);
        c.call();
        c.call();
        c.call();
        c.call();
        assertEquals(3, c.getMaxAndReset());

        assertEquals(0, c.getMaxAndReset());
    }

    @Test
    public void name1() throws Exception {

    }
}