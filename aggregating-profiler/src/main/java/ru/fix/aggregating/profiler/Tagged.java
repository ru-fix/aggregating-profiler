package ru.fix.aggregating.profiler;

import java.util.Map;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagged {
    static final String GRAPHITE_SELECTOR = "graphite";
    Map<String, String> getTags();
}
