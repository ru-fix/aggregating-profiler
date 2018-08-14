package ru.fix.aggregating.profiler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagger {
    static final String DEFAULT_GRAPHITE_TAG_VALUE = "default";
    <T extends Tagged> T setTag(String profiledCallName, T obj);
    <T extends Tagged> T setTag(String tagName, String profiledCallName, T obj);
}
