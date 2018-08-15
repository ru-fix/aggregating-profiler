package ru.fix.aggregating.profiler;

/**
 *
 * @author Andrey Kiselev
 */

public class DefaultTagger implements Tagger {
    @Override
    public <T extends Tagged> T assignTag(String profiledCallName,
                                          T obj) {
        return assignTag(Tagged.DEFAULT_TAG_KEY,
                         profiledCallName,
                         obj);
    }

    @Override
    public <T extends Tagged> T assignTag(String tagName,
                                          String profiledCallName,
                                          T obj) {
        obj.setTag(tagName, Tagger.DEFAULT_TAG_VALUE);
        return obj;
    }

}
