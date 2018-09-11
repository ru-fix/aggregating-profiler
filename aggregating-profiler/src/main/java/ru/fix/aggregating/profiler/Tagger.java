package ru.fix.aggregating.profiler;

/**
 *
 * @author Andrey Kiselev
 */

public interface Tagger {
    <T extends Tagged> T assignTag(String profiledCallName, T tagged);
}
