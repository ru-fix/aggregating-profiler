package ru.fix.aggregating.profiler;

/**
 * Fake implementation of Tagger.
 * It does not tag anything
 */
public class NullTagger implements Tagger {

    @Override
    public <T extends Tagged> T assignTag(String profiledCallName, T obj) {
        return obj;
    }

    @Override
    public <T extends Tagged> T assignTag(String tagName, String profiledCallName, T obj) {
        return obj;
    }
}
