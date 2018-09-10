package ru.fix.aggregating.profiler;

/**
 * Stub implementation of Tagger.
 * It does not tag anything
 */
public class NoopTagger implements Tagger {

    @Override
    public <T extends Tagged> T assignTag(String profiledCallName, T tagged) {
        return tagged;
    }

    @Override
    public <T extends Tagged> T assignTag(String tagName, String profiledCallName, T tagged) {
        return tagged;
    }
}
