package ru.fix.aggregating.profiler;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagger {
    public static final String EMPTY_VALUE = "";
    <T extends Tagged> T assignTag(String profiledCallName, T tagged);
}
