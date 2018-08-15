package ru.fix.aggregating.profiler;

import java.util.Map;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagged {
    static final String DEFAULT_TAG_KEY = "defkey";
    Map<String, String> getTags();
    void setTag(String name, String value);
}
