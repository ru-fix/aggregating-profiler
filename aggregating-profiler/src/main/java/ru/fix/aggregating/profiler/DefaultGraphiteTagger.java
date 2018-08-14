package ru.fix.aggregating.profiler;

/**
 *
 * @author Andrey Kiselev
 */

public class DefaultGraphiteTagger implements Tagger {
    @Override
    public <T extends Tagged> T setTag(String profiledCallName,
                                       T obj) {
        return setTag(Tagged.GRAPHITE_SELECTOR,
                      profiledCallName,
                      obj);
    }

    @Override
    public <T extends Tagged> T setTag(String tagName,
                                       String profiledCallName,
                                       T obj) {
        obj.getTags().put(tagName, Tagger.DEFAULT_GRAPHITE_TAG_VALUE);
        return obj;
    }

}
