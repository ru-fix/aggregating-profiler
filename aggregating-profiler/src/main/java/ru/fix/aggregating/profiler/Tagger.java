package ru.fix.aggregating.profiler;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagger {
    static final String DEFAULT_TAG_VALUE = "defval";
    <T extends Tagged> T assignTag(String profiledCallName, T obj);
    <T extends Tagged> T assignTag(String tagName, String profiledCallName, T obj);
}
