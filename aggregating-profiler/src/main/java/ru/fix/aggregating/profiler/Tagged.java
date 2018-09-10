package ru.fix.aggregating.profiler;

import java.util.Map;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagged {
    Map<String, String> getTags(); 
    boolean hasTag(String tagName, String tagValue);
    void setTag(String name, String value);
}
