package ru.fix.aggregating.profiler;

/**
 *
 * @author Andrey Kiselev
 */

public class DefaultTagger implements Tagger {
    @Override
    public <T extends Tagged> T assignTag(String profiledCallName,
                                          T tagged) {
        return assignTag(Tagged.DEFAULT_TAG_KEY,
                         profiledCallName,
                         tagged);
    }

    @Override
    public <T extends Tagged> T assignTag(String tagName,
                                          String profiledCallName,
                                          T tagged) {
        tagged.setTag(tagName, Tagger.DEFAULT_TAG_VALUE);
        return tagged;
    }

}
